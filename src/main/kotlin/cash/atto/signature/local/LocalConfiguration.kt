package cash.atto.signature.local

import cash.atto.ApplicationProperties
import cash.atto.commons.AttoPrivateKey
import cash.atto.commons.AttoSigner
import cash.atto.commons.fromHexToByteArray
import cash.atto.commons.toSigner
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["atto.backend"], havingValue = "LOCAL")
class LocalConfiguration {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun localSigner(properties: ApplicationProperties): AttoSigner {
        logger.info {"Backend ${properties.backend} configured"}
        val privateKey = AttoPrivateKey(properties.key.fromHexToByteArray())
        return privateKey.toSigner()
    }
}
