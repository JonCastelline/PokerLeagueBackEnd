package com.pokerleaguebackend.controller

import com.pokerleaguebackend.payload.ApiResponse
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.JwtAuthenticationResponse
import com.pokerleaguebackend.payload.LoginRequest
import com.pokerleaguebackend.payload.SignUpRequest
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val playerAccountRepository: PlayerAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: JwtTokenProvider
) {

    @PostMapping("/signin")
    fun authenticateUser(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.email,
                loginRequest.password
            )
        )

        SecurityContextHolder.getContext().authentication = authentication

        val jwt = tokenProvider.generateToken(authentication)
        return ResponseEntity.ok(JwtAuthenticationResponse(jwt))
    }

    @PostMapping("/signup")
    fun registerUser(@Valid @RequestBody signUpRequest: SignUpRequest): ResponseEntity<ApiResponse> {
        if (playerAccountRepository.findByEmail(signUpRequest.email) != null) {
            return ResponseEntity(ApiResponse(false, "Email address already in use!"), HttpStatus.BAD_REQUEST)
        }

        val playerAccount = PlayerAccount(
            firstName = signUpRequest.firstName,
            lastName = signUpRequest.lastName,
            email = signUpRequest.email,
            password = passwordEncoder.encode(signUpRequest.password)
        )

        val result = playerAccountRepository.save(playerAccount)

        val location: URI = ServletUriComponentsBuilder
            .fromCurrentContextPath().path("/api/users/{email}")
            .buildAndExpand(result.email).toUri()

        return ResponseEntity.created(location).body(ApiResponse(true, "User registered successfully!"))
    }
}
