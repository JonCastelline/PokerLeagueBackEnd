package com.pokerleaguebackend.security

import com.pokerleaguebackend.service.CustomUserDetailsService
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenProvider: JwtTokenProvider,
    private val customUserDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    private val jwtLogger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator") || (path.startsWith("/api/games/") && path.endsWith("/calendar.ics"))
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            getJwtFromRequest(request)?.let { jwt ->
                if (StringUtils.hasText(jwt)) {
                    tokenProvider.validateToken(jwt)
                    val userEmail = tokenProvider.getEmailFromJWT(jwt)
                    val userDetails = customUserDetailsService.loadUserByUsername(userEmail)
                    val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
            filterChain.doFilter(request, response)
        } catch (ex: ExpiredJwtException) {
            jwtLogger.error("Expired JWT token: {}", ex.message)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("{\"error\": \"Expired JWT token\"}")
            return
        } catch (ex: Exception) {
            jwtLogger.error("Authentication error: {}", ex.message)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("{\"error\": \"Invalid Token\"}")
            return
        }
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        jwtLogger.debug("Attempting to get JWT from request. Authorization header: {}", bearerToken)
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            val jwt = bearerToken.substring(7, bearerToken.length)
            jwtLogger.debug("Extracted JWT: {}", jwt)
            jwt
        } else {
            jwtLogger.debug("Authorization header does not contain a Bearer token.")
            null
        }
    }
}
