package cash.atto.signature

import cash.atto.ApplicationProperties
import cash.atto.commons.AttoAddress
import cash.atto.commons.AttoAlgorithm
import cash.atto.commons.AttoAmount
import cash.atto.commons.AttoBlock
import cash.atto.commons.AttoChangeBlock
import cash.atto.commons.AttoHash
import cash.atto.commons.AttoHeight
import cash.atto.commons.AttoInstant
import cash.atto.commons.AttoNetwork
import cash.atto.commons.AttoOpenBlock
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoReceiveBlock
import cash.atto.commons.AttoSendBlock
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoSigner
import cash.atto.commons.AttoVote
import cash.atto.commons.isValidBlocking
import cash.atto.commons.toAttoVersion
import cash.atto.signature.SignatureController.BlockSignatureRequest
import cash.atto.signature.SignatureController.SignatureResponse
import cash.atto.signature.SignatureController.VoteSignatureRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["management.server.port=0", "atto.capabilities=BLOCK,VOTE,CHALLENGE"],
)
@AutoConfigureTestRestTemplate
class CanonicalSignatureHttpTest
    @Autowired
    constructor(
        private val client: TestRestTemplate,
        private val signer: AttoSigner,
        private val properties: ApplicationProperties,
    ) {
        @Test
        fun `canonical and legacy blocks preserve signatures and reject incomplete role identities`() {
            // Given
            val other = AttoAddress(AttoAlgorithm.V1, AttoPublicKey(ByteArray(32) { 2 }))
            val hash = AttoHash(ByteArray(32) { 3 })
            val timestamp = AttoInstant.fromEpochMilliseconds(1)
            val version = 0U.toAttoVersion()
            val blocks =
                listOf<AttoBlock>(
                    AttoOpenBlock(
                        AttoNetwork.LOCAL,
                        version,
                        signer.address,
                        AttoAmount(10UL),
                        timestamp,
                        AttoAlgorithm.V1,
                        hash,
                        other,
                    ),
                    AttoReceiveBlock(
                        AttoNetwork.LOCAL,
                        version,
                        signer.address,
                        AttoHeight(2UL),
                        AttoAmount(10UL),
                        timestamp,
                        hash,
                        AttoAlgorithm.V1,
                        hash,
                    ),
                    AttoSendBlock(
                        AttoNetwork.LOCAL,
                        version,
                        signer.address,
                        AttoHeight(2UL),
                        AttoAmount(10UL),
                        timestamp,
                        hash,
                        other,
                        AttoAmount(3UL),
                    ),
                    AttoChangeBlock(
                        AttoNetwork.LOCAL,
                        version,
                        signer.address,
                        AttoHeight(2UL),
                        AttoAmount(10UL),
                        timestamp,
                        hash,
                        other,
                    ),
                )

            for (block in blocks) {
                val canonical = Json.encodeToJsonElement(BlockSignatureRequest(block)).jsonObject
                val target = canonical.getValue("target").jsonObject
                val legacy = buildJsonObject { put("target", legacyIdentity(target)) }

                // When
                val canonicalSignature = sign("/blocks", canonical)
                val legacySignature = sign("/blocks", legacy)
                val duplicateSignature = sign("/blocks", buildJsonObject { put("target", malformedDuplicateIdentity(target)) })

                // Then
                assertFalse(target.containsKey("publicKey"))
                assertFalse(target.containsKey("algorithm"))
                assertEquals(canonicalSignature, legacySignature)
                assertEquals(canonicalSignature, duplicateSignature)
                assertTrue(canonicalSignature.isValidBlocking(signer.address.publicKey, block.hash))

                for (prefix in listOf("representative", "receiver")) {
                    if ("${prefix}Address" !in target) continue
                    for (missingField in listOf("${prefix}Algorithm", "${prefix}PublicKey")) {
                        val incomplete = legacyIdentity(target).toMutableMap().apply { remove(missingField) }
                        val body = buildJsonObject { put("target", JsonObject(incomplete)) }.toString()
                        val headers =
                            HttpHeaders().apply {
                                contentType = MediaType.APPLICATION_JSON
                                set(HttpHeaders.AUTHORIZATION, properties.token)
                            }

                        // When
                        val response = client.postForEntity("/blocks", HttpEntity(body, headers), String::class.java)

                        // Then
                        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, body + response.body)
                    }
                }
            }
        }

        @Test
        fun `canonical and legacy votes produce the same valid signature`() {
            // Given
            val vote =
                AttoVote(
                    version = 0U.toAttoVersion(),
                    address = signer.address,
                    blockAlgorithm = AttoAlgorithm.V1,
                    blockHash = AttoHash(ByteArray(32) { 4 }),
                    timestamp = AttoInstant.fromEpochMilliseconds(1),
                )
            val canonical = Json.encodeToJsonElement(VoteSignatureRequest(vote)).jsonObject
            val target = canonical.getValue("target").jsonObject
            val legacy = buildJsonObject { put("target", legacyIdentity(target)) }

            // When
            val canonicalSignature = sign("/votes", canonical)
            val legacySignature = sign("/votes", legacy)
            val duplicateSignature = sign("/votes", buildJsonObject { put("target", malformedDuplicateIdentity(target)) })

            // Then
            assertFalse(target.containsKey("publicKey"))
            assertFalse(target.containsKey("algorithm"))
            assertTrue(target.containsKey("blockAlgorithm"))
            assertEquals(canonicalSignature, legacySignature)
            assertEquals(canonicalSignature, duplicateSignature)
            assertTrue(canonicalSignature.isValidBlocking(signer.address.publicKey, vote.hash))
        }

        @Test
        fun `canonical and legacy votes ignore unknown block role fields`() {
            // Given
            val vote =
                AttoVote(
                    version = 0U.toAttoVersion(),
                    address = signer.address,
                    blockAlgorithm = AttoAlgorithm.V1,
                    blockHash = AttoHash(ByteArray(32) { 4 }),
                    timestamp = AttoInstant.fromEpochMilliseconds(1),
                )
            val canonical = Json.encodeToJsonElement(VoteSignatureRequest(vote)).jsonObject
            val target = canonical.getValue("target").jsonObject
            val expected = sign("/votes", canonical)

            for (identity in listOf(target, legacyIdentity(target))) {
                for ((type, prefix) in listOf("SEND" to "receiver", "OPEN" to "representative", "CHANGE" to "representative")) {
                    for ((field, value) in mapOf(
                        "${prefix}Algorithm" to JsonPrimitive("V1"),
                        "${prefix}PublicKey" to JsonPrimitive(signer.address.publicKey.toString()),
                    )) {
                        val extended = JsonObject(identity + mapOf("type" to JsonPrimitive(type), field to value))
                        val body = buildJsonObject { put("target", extended) }

                        // When
                        val signature = sign("/votes", body)

                        // Then
                        assertEquals(expected, signature, body.toString())
                    }
                }
            }
        }

        @Test
        fun `canonical signing retains token authentication`() {
            // Given
            val vote =
                AttoVote(
                    0U.toAttoVersion(),
                    signer.address,
                    AttoAlgorithm.V1,
                    AttoHash(ByteArray(32) { 4 }),
                    AttoInstant.fromEpochMilliseconds(1),
                )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val canonical = Json.encodeToJsonElement(VoteSignatureRequest(vote)).jsonObject
            val legacy = buildJsonObject { put("target", legacyIdentity(canonical.getValue("target").jsonObject)) }

            // When / Then
            for (body in listOf(canonical, legacy)) {
                val response = client.postForEntity("/votes", HttpEntity(body.toString(), headers), String::class.java)
                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            }
        }

        @Test
        fun `legacy signing rejects incomplete invalid and absent identity pairs`() {
            // Given
            val block =
                AttoOpenBlock(
                    AttoNetwork.LOCAL,
                    0U.toAttoVersion(),
                    signer.address,
                    AttoAmount(10UL),
                    AttoInstant.fromEpochMilliseconds(1),
                    AttoAlgorithm.V1,
                    AttoHash(ByteArray(32) { 3 }),
                    signer.address,
                )
            val vote =
                AttoVote(
                    0U.toAttoVersion(),
                    signer.address,
                    AttoAlgorithm.V1,
                    AttoHash(ByteArray(32) { 4 }),
                    AttoInstant.fromEpochMilliseconds(1),
                )
            val cases =
                listOf(
                    "/blocks" to
                        Json
                            .encodeToJsonElement(BlockSignatureRequest(block))
                            .jsonObject
                            .getValue("target")
                            .jsonObject,
                    "/votes" to
                        Json
                            .encodeToJsonElement(VoteSignatureRequest(vote))
                            .jsonObject
                            .getValue("target")
                            .jsonObject,
                )
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set(HttpHeaders.AUTHORIZATION, properties.token)
                }

            // When / Then
            for ((path, canonical) in cases) {
                val legacy = legacyIdentity(canonical)
                val rejected =
                    listOf(
                        legacy.toMutableMap().apply { remove("algorithm") },
                        legacy.toMutableMap().apply { remove("publicKey") },
                        legacy.toMutableMap().apply {
                            remove("algorithm")
                            remove("publicKey")
                        },
                        legacy.toMutableMap().apply { put("algorithm", JsonPrimitive("invalid")) },
                        legacy.toMutableMap().apply { put("publicKey", JsonPrimitive("invalid")) },
                        canonical.toMutableMap().apply { put("address", JsonPrimitive("invalid")) },
                        legacy.toMutableMap().apply { put("address", JsonNull) },
                    )
                for (target in rejected) {
                    val body = buildJsonObject { put("target", JsonObject(target)) }.toString()
                    val response = client.postForEntity(path, HttpEntity(body, headers), String::class.java)
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, body + response.body)
                }
            }
        }

        private fun malformedDuplicateIdentity(canonical: JsonObject): JsonObject {
            val fields = canonical.toMutableMap()
            for ((addressName, prefix) in listOf(
                "address" to "",
                "representativeAddress" to "representative",
                "receiverAddress" to "receiver",
            )) {
                if (addressName !in fields) continue
                fields[if (prefix.isEmpty()) "algorithm" else "${prefix}Algorithm"] = JsonPrimitive("invalid")
                fields[if (prefix.isEmpty()) "publicKey" else "${prefix}PublicKey"] = JsonObject(emptyMap())
            }
            return JsonObject(fields)
        }

        private fun sign(
            path: String,
            body: JsonObject,
        ): AttoSignature {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set(HttpHeaders.AUTHORIZATION, properties.token)
                }
            val response = client.postForEntity(path, HttpEntity(body.toString(), headers), String::class.java)
            assertEquals(HttpStatus.OK, response.statusCode, response.body)
            return Json.decodeFromString<SignatureResponse>(requireNotNull(response.body)).signature
        }

        private fun legacyIdentity(canonical: JsonObject): JsonObject {
            val fields = canonical.toMutableMap()
            for ((addressName, prefix) in listOf(
                "address" to "",
                "representativeAddress" to "representative",
                "receiverAddress" to "receiver",
            )) {
                val encoded = fields.remove(addressName) ?: continue
                val address = AttoAddress.parse(encoded.jsonPrimitive.content)
                val algorithmName = if (prefix.isEmpty()) "algorithm" else "${prefix}Algorithm"
                val keyName = if (prefix.isEmpty()) "publicKey" else "${prefix}PublicKey"
                fields[algorithmName] = JsonPrimitive(address.algorithm.name)
                fields[keyName] = JsonPrimitive(address.publicKey.toString())
            }
            if (fields["type"]?.jsonPrimitive?.content == "OPEN") fields.remove("height")
            for (name in listOf("balance", "height", "timestamp", "amount")) {
                fields[name]?.let { fields[name] = Json.parseToJsonElement(it.jsonPrimitive.content) }
            }
            return JsonObject(fields)
        }
    }
