package cash.atto.signature

import cash.atto.ApplicationProperties
import cash.atto.Capability
import cash.atto.commons.AttoBlock
import cash.atto.commons.AttoChallenge
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoSigner
import cash.atto.commons.AttoVote
import kotlinx.datetime.Instant
import org.springframework.stereotype.Service

@Service
class SignatureService(
    private val signer: AttoSigner,
    private val properties: ApplicationProperties,
) {
    val publicKey by lazy { signer.publicKey }

    suspend fun sign(block: AttoBlock): AttoSignature {
        require(properties.capabilities.contains(Capability.BLOCK)) {
            "Signing a block is not allowed. Capability BLOCK is missing."
        }
        return signer.sign(block)
    }

    suspend fun sign(vote: AttoVote): AttoSignature {
        require(properties.capabilities.contains(Capability.VOTE)) {
            "Signing a vote is not allowed. Capability VOTE is missing."
        }
        return signer.sign(vote)
    }

    suspend fun sign(
        challenge: AttoChallenge,
        timestamp: Instant,
    ): AttoSignature {
        require(properties.capabilities.contains(Capability.CHALLENGE)) {
            "Signing a challenge is not allowed. Capability CHALLENGE is missing."
        }
        return signer.sign(challenge, timestamp)
    }
}
