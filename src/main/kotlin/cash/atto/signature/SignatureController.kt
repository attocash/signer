package cash.atto.signature

import cash.atto.commons.AttoBlock
import cash.atto.commons.AttoChallenge
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoVote
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.Serializable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class SignatureController(
    private val service: SignatureService,
) {
    @PostMapping("/blocks")
    suspend fun signBlock(
        @RequestBody request: Request<AttoBlock>,
    ): SignatureResponse {
        return service.sign(request.target).toResponse()
    }

    @PostMapping("/votes")
    suspend fun signVote(
        @RequestBody request: Request<AttoVote>,
    ): SignatureResponse = service.sign(request.target).toResponse()

    @PostMapping("/challenges")
    suspend fun signChallenge(
        @RequestBody request: Request<AttoChallenge>,
    ): SignatureResponse = service.sign(request.target).toResponse()

    @Serializable
    data class Request<T>(
        val target: T,
    )

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
