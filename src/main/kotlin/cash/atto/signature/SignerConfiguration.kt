package cash.atto.signature

import cash.atto.ApplicationProperties
import cash.atto.BackendType
import cash.atto.commons.AttoHash
import cash.atto.commons.AttoPrivateKey
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoSigner
import cash.atto.commons.fromHexToByteArray
import cash.atto.commons.toSigner
import com.google.cloud.kms.v1.AsymmetricSignRequest
import com.google.cloud.kms.v1.KeyManagementServiceClient
import com.google.protobuf.ByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.Closeable
import java.util.Base64
import java.util.concurrent.Executors

@Configuration
class SignerConfiguration {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun signer(properties: ApplicationProperties): AttoSigner {
        val backend = properties.backend
        logger.info { "Backend $backend configured" }
        return when (backend) {
            BackendType.LOCAL -> {
                val privateKey = AttoPrivateKey(properties.key.fromHexToByteArray())
                privateKey.toSigner()
            }

            BackendType.GCP -> {
                val client = KeyManagementServiceClient.create()
                GCPSigner(client, properties.key)
            }
        }
    }

    private class GCPSigner(
        private val client: KeyManagementServiceClient,
        val key: String,
    ) : AttoSigner,
        Closeable {
        override val publicKey: AttoPublicKey by lazy {
            val pem = client.getPublicKey(key).pem

            val cleanedPem =
                pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("\\s".toRegex(), "")

            AttoPublicKey(Base64.getDecoder().decode(cleanedPem).sliceArray(12 until 44))
        }

        val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

        override suspend fun sign(hash: AttoHash): AttoSignature {
            val request =
                AsymmetricSignRequest
                    .newBuilder()
                    .setName(key)
                    .setData(ByteString.copyFrom(hash.value))
                    .build()

            val response =
                withContext(dispatcher) {
                    client.asymmetricSign(request)
                }

            return AttoSignature(response.signature.toByteArray())
        }

        override fun close() {
            client.close()
        }
    }
}
