package cash.atto.signature.local

import cash.atto.ApplicationProperties
import cash.atto.commons.AttoPrivateKey
import cash.atto.commons.AttoSigner
import cash.atto.commons.fromHexToByteArray
import cash.atto.commons.toSigner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["atto.backend"], havingValue = "LOCAL")
class LocalConfiguration {
    @Bean
    fun localSigner(properties: ApplicationProperties): AttoSigner {
        val privateKey = AttoPrivateKey(properties.key.fromHexToByteArray())
        return privateKey.toSigner()
    }
}
