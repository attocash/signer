package cash.atto.signature

import cash.atto.ApplicationProperties
import cash.atto.CacheSupport
import cash.atto.Capability
import cash.atto.commons.AttoAlgorithm
import cash.atto.commons.AttoAmount
import cash.atto.commons.AttoBlock
import cash.atto.commons.AttoChallenge
import cash.atto.commons.AttoHash
import cash.atto.commons.AttoNetwork
import cash.atto.commons.AttoOpenBlock
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoSigner
import cash.atto.commons.AttoVote
import cash.atto.commons.isValid
import cash.atto.commons.toAttoVersion
import cash.atto.signature.SignatureController.SignatureRequest
import cash.atto.signature.SignatureController.SignatureResponse
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.fail
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import kotlin.random.Random

class SignatureStepDefinition(
    private val properties: ApplicationProperties,
    private val signer: AttoSigner,
    private val testRestTemplate: TestRestTemplate,
) : CacheSupport {
    private var hash: AttoHash? = null
    private var signature: AttoSignature? = null

    @Given("signer has {word} capability")
    fun setCapability(capability: String) {
        properties.capabilities = setOf(Capability.valueOf(capability))
    }

    @When("block is signed")
    fun signBlock() {
        val block =
            AttoOpenBlock(
                version = 0U.toAttoVersion(),
                network = AttoNetwork.LOCAL,
                algorithm = AttoAlgorithm.V1,
                publicKey = AttoPublicKey(Random.Default.nextBytes(ByteArray(32))),
                balance = AttoAmount.MAX,
                timestamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()),
                sendHashAlgorithm = AttoAlgorithm.V1,
                sendHash = AttoHash(Random.Default.nextBytes(ByteArray(32))),
                representativeAlgorithm = AttoAlgorithm.V1,
                representativePublicKey = AttoPublicKey(Random.Default.nextBytes(ByteArray(32))),
            )

        val request = Json.encodeToString(SignatureRequest.serializer(AttoBlock.serializer()), SignatureRequest(block))
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", properties.token)
            }
        val entity = HttpEntity(request, headers)

        val response = testRestTemplate.postForObject("/blocks", entity, String::class.java)
        hash = block.hash
        signature = Json.decodeFromString<SignatureResponse>(response).signature
    }

    @When("block is not signed")
    fun illegalSignBlock() {
        try {
            signBlock()
            fail { "Block signing should have faild" }
        } catch (e: Exception) {
        }
    }

    @When("vote is signed")
    fun signVote() {
        val vote =
            AttoVote(
                blockAlgorithm = AttoAlgorithm.V1,
                blockHash = AttoHash(Random.Default.nextBytes(ByteArray(32))),
                timestamp = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()),
            )

        val request = Json.encodeToString(SignatureRequest.serializer(AttoVote.serializer()), SignatureRequest(vote))
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", properties.token)
            }
        val entity = HttpEntity(request, headers)

        val response = testRestTemplate.postForObject("/votes", entity, String::class.java)
        hash = vote.hash
        signature = Json.decodeFromString<SignatureResponse>(response).signature
    }

    @When("challenge is signed")
    fun signChallenge() {
        val challenge = AttoChallenge(ByteArray(64))

        val request = Json.encodeToString(SignatureRequest.serializer(AttoChallenge.serializer()), SignatureRequest(challenge))
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", properties.token)
            }
        val entity = HttpEntity(request, headers)

        val response = testRestTemplate.postForObject("/challenges", entity, String::class.java)
        hash = AttoHash.hash(64, signer.publicKey.value, challenge.value)
        signature = Json.decodeFromString<SignatureResponse>(response).signature
    }

    @Then("signature is valid")
    fun check() {
        assertTrue(signature!!.isValid(signer.publicKey, hash!!))
    }

    override fun clear() {
        signature = null
    }
}
