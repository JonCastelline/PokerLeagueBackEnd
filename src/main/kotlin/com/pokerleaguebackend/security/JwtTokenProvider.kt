package com.pokerleaguebackend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.Date
import org.springframework.core.env.Environment

@Component
class JwtTokenProvider(private val env: Environment) {

    val jwtSecret: String = env.getProperty("jwtSecret") ?: throw IllegalArgumentException("jwtSecret not found")
    val jwtExpirationInMs: Long = env.getProperty("jwtExpirationInMs")?.toLong() ?: throw IllegalArgumentException("jwtExpirationInMs not found")

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationInMs)

        return Jwts.builder()
            .setSubject(userPrincipal.username)
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact()
    }

    fun getUsernameFromJWT(token: String): String {
        val claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .body

        return claims.subject
    }

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken)
            return true
        } catch (ex: Exception) {
            // Log exception
        }
        return false
    }
}