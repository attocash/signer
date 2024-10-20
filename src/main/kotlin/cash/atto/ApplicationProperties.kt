package cash.atto

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "atto")
class ApplicationProperties {
    lateinit var token: String
    lateinit var capabilities: Set<Capability>
    lateinit var backend: BackendType
    lateinit var key: String
}
