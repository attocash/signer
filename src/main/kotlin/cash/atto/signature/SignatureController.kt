package cash.atto.signature

import cash.atto.commons.AttoBlock
import cash.atto.commons.AttoChallenge
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoVote
import cash.atto.commons.serialiazer.InstantMillisSerializer
import jakarta.servlet.http.HttpServletRequest
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class SignatureController(
    private val service: SignatureService,
) {
    @GetMapping("/public-keys")
    suspend fun getPublicKey(): PublicKeyResponse = PublicKeyResponse(service.publicKey)

    @PostMapping("/blocks")
    suspend fun signBlock(
        @RequestBody request: BlockSignatureRequest,
    ): SignatureResponse = service.sign(request.target).toResponse()

    @PostMapping("/votes")
    suspend fun signVote(
        @RequestBody request: VoteSignatureRequest,
    ): SignatureResponse = service.sign(request.target).toResponse()

    @PostMapping("/challenges")
    suspend fun signChallenge(
        @RequestBody request: ChallengeSignatureRequest,
    ): SignatureResponse = service.sign(request.target, request.timestamp).toResponse()

    @Serializable
    data class PublicKeyResponse(
        val publicKey: AttoPublicKey,
    )

    interface SignatureRequest<T> {
        val target: T
    }

    @Serializable
    data class BlockSignatureRequest(
        override val target: AttoBlock,
    ) : SignatureRequest<AttoBlock>

    @Serializable
    data class VoteSignatureRequest(
        override val target: AttoVote,
    ) : SignatureRequest<AttoVote>

    @Serializable
    data class ChallengeSignatureRequest(
        override val target: AttoChallenge,
        @Serializable(with = InstantMillisSerializer::class)
        val timestamp: Instant,
    ) : SignatureRequest<AttoChallenge>

    @Serializable
    data class SignatureResponse(
        val signature: AttoSignature,
    )

    private fun AttoSignature.toResponse() = SignatureResponse(this)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleError(
        req: HttpServletRequest,
        e: IllegalArgumentException,
    ): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to e.message))
}
