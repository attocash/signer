package cash.atto

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TokenValidationFilter(
    val properties: ApplicationProperties,
    val managementPort: ManagementServerProperties,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val localPort = request.localPort

        if (request.method == "GET" || localPort == managementPort.port) {
            filterChain.doFilter(request, response)
            return
        }

        val token = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (token == properties.token) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.UNAUTHORIZED.value()
        }
    }
}
