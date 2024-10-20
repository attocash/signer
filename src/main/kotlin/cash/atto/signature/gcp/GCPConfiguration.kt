package cash.atto.signature.gcp

import cash.atto.ApplicationProperties
import cash.atto.commons.AttoHash
import cash.atto.commons.AttoPublicKey
import cash.atto.commons.AttoSignature
import cash.atto.commons.AttoSigner
import com.google.cloud.kms.v1.AsymmetricSignRequest
import com.google.cloud.kms.v1.KeyManagementServiceClient
import com.google.protobuf.ByteString
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.Closeable
import java.util.Base64
import java.util.concurrent.Executors

@Configuration
@ConditionalOnProperty(name = ["atto.backend"], havingValue = "GCP")
class GCPConfiguration {
    @Bean
    fun gcpSigner(properties: ApplicationProperties): AttoSigner {
        val client = KeyManagementServiceClient.create()
        return GCPSigner(client, properties.key)
    }
}


private class GCPSigner(private val client: KeyManagementServiceClient, val key: String) : AttoSigner, Closeable {
    override val publicKey: AttoPublicKey by lazy {
        val pem = client.getPublicKey(key).pem

        val cleanedPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        AttoPublicKey(Base64.getDecoder().decode(cleanedPem).sliceArray(12 until 44))
    }

    val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

    override suspend fun sign(hash: AttoHash): AttoSignature {
        val request = AsymmetricSignRequest.newBuilder()
            .setName(key)
            .setData(ByteString.copyFrom(hash.value))
            .build()

        val response = withContext(dispatcher) {
            client.asymmetricSign(request)
        }

        return AttoSignature(response.signature.toByteArray())

    }

    override fun close() {
        client.close()
    }

}
