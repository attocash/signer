package cash.atto

import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class TokenValidationFilter(
    private val properties: ApplicationProperties,
    private val managementPort: ManagementServerProperties,
) : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val request = exchange.request
        val localPort = request.localAddress?.port

        if (request.method == HttpMethod.GET || localPort == managementPort.port) {
            return chain.filter(exchange)
        }

        val token = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        return if (token == properties.token) {
            chain.filter(exchange)
        } else {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete()
        }
    }
}
