package com.pokerleaguebackend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    @Value("\${app.jwtSecret}")
    private lateinit var jwtSecret: String

    @Value("\${app.jwtExpirationInMs}")
    private var jwtExpirationInMs: Int = 0

    private var secretKey: SecretKey? = null

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    fun getSecretKey(): SecretKey {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        }
        return secretKey as SecretKey
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        return generateToken(userPrincipal.username)
    }

    fun generateToken(email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationInMs)

        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(getSecretKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun getEmailFromJWT(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(getSecretKey())
            .build()
            .parseClaimsJws(token)
            .body

        return claims.subject
    }

    fun validateToken(authToken: String) {
        try {
            Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(authToken)
        } catch (ex: Exception) {
            logger.error("Invalid JWT token", ex)
            throw ex // Re-throw the exception to be handled by the filter
        }
    }
}
