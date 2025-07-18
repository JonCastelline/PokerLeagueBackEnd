package com.example.pokerleaguebackend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    @Value("\${app.jwtSecret}")
    private lateinit var jwtSecret: String

    @Value("\${app.jwtExpirationInMs}")
    private var jwtExpirationInMs: Int = 0

    private var secretKey: SecretKey? = null

    fun getSecretKey(): SecretKey {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        }
        return secretKey as SecretKey
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationInMs)

        return Jwts.builder()
            .setSubject(userPrincipal.username)
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

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(authToken)
            return true
        } catch (ex: Exception) {
            // log exception
        }
        return false
    }
}
