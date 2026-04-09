package cash.atto.signature

import cash.atto.ApplicationProperties
import cash.atto.CacheSupport
import cash.atto.Capability
import cash.atto.commons.AttoAlgorithm
import cash.atto.commons.AttoAmount
import cash.atto.commons.AttoChallenge
import cash.atto.commons.AttoHash
import cash.atto.commons.AttoInstant
import cash.atto.commons.AttoNetwork
import cash.atto.commons.AttoOpenBlock
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoSigner
import cash.atto.commons.AttoVote
import cash.atto.commons.isValid
import cash.atto.commons.toAttoVersion
import cash.atto.commons.toByteArray
import cash.atto.signature.SignatureController.BlockSignatureRequest
import cash.atto.signature.SignatureController.ChallengeSignatureRequest
import cash.atto.signature.SignatureController.SignatureResponse
import cash.atto.signature.SignatureController.VoteSignatureRequest
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import kotlin.random.Random

class SignatureStepDefinition(
    private val properties: ApplicationProperties,
    private val signer: AttoSigner,
    private val testRestTemplate: TestRestTemplate,
) : CacheSupport {
    private var hash: AttoHash? = null
    private var signature: AttoSignature? = null
    private var response: ResponseEntity<String>? = null

    @Given("signer has {word} capability")
    fun setCapability(capability: String) {
        properties.capabilities = setOf(Capability.valueOf(capability))
    }

    @When("block is signed")
    fun signBlock() {
        val block = buildBlock()
        response = postBlock(block)

        assertEquals(HttpStatus.OK, response!!.statusCode)
        hash = block.hash
        signature = Json.decodeFromString<SignatureResponse>(response!!.body!!).signature
    }

    @When("block is not signed")
    fun illegalSignBlock() {
        val block = buildBlock()
        response = postBlock(block)

        assertEquals(HttpStatus.BAD_REQUEST, response!!.statusCode)
    }

    @When("block is signed with a wrong token")
    fun signBlockWithWrongToken() {
        val block = buildBlock()
        response = postBlock(block, "wrong_token")
    }

    @When("vote is signed")
    fun signVote() {
        val vote =
            AttoVote(
                version = 0U.toAttoVersion(),
                algorithm = AttoAlgorithm.V1,
                publicKey = signer.publicKey,
                blockAlgorithm = AttoAlgorithm.V1,
                blockHash = AttoHash(Random.Default.nextBytes(ByteArray(32))),
                timestamp = AttoInstant.now(),
            )

        val request = Json.encodeToString(VoteSignatureRequest.serializer(), VoteSignatureRequest(vote))
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", properties.token)
            }
        val entity = HttpEntity(request, headers)

        response = testRestTemplate.postForEntity<String>("/votes", entity)
        assertEquals(HttpStatus.OK, response!!.statusCode)
        hash = vote.hash
        signature = Json.decodeFromString<SignatureResponse>(response!!.body!!).signature
    }

    @When("challenge is signed")
    fun signChallenge() {
        val timestamp = AttoInstant.now()
        val challenge = AttoChallenge(ByteArray(64))

        val request = Json.encodeToString(ChallengeSignatureRequest.serializer(), ChallengeSignatureRequest(challenge, timestamp))
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", properties.token)
            }
        val entity = HttpEntity(request, headers)

        response = testRestTemplate.postForEntity<String>("/challenges", entity)
        assertEquals(HttpStatus.OK, response!!.statusCode)
        hash = AttoHash.hash(64, signer.publicKey.value, challenge.value, timestamp.toByteArray())
        signature = Json.decodeFromString<SignatureResponse>(response!!.body!!).signature
    }

    @Then("signature is valid")
    fun check() {
        assertTrue(signature!!.isValid(signer.publicKey, hash!!))
    }

    @Then("request is unauthorized")
    fun requestIsUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, response!!.statusCode)
    }

    override fun clear() {
        response = null
        signature = null
    }

    private fun buildBlock() =
        AttoOpenBlock(
            version = 0U.toAttoVersion(),
            network = AttoNetwork.LOCAL,
            algorithm = AttoAlgorithm.V1,
            publicKey = signer.publicKey,
            balance = AttoAmount.MAX,
            timestamp = AttoInstant.now(),
            sendHashAlgorithm = AttoAlgorithm.V1,
            sendHash = AttoHash(Random.Default.nextBytes(ByteArray(32))),
            representativeAlgorithm = AttoAlgorithm.V1,
            representativePublicKey = AttoPublicKey(Random.Default.nextBytes(ByteArray(32))),
        )

    private fun postBlock(
        block: AttoOpenBlock,
        token: String = properties.token,
    ): ResponseEntity<String> {
        val request = Json.encodeToString(BlockSignatureRequest.serializer(), BlockSignatureRequest(block))
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", token)
            }
        val entity = HttpEntity(request, headers)

        return testRestTemplate.postForEntity("/blocks", entity, String::class.java)
    }
}
