package com.pokerleaguebackend.controller

import com.pokerleaguebackend.exception.LeagueNotFoundException
import com.pokerleaguebackend.payload.dto.PublicPlayerInviteDto
import com.pokerleaguebackend.payload.request.SignUpRequest
import com.pokerleaguebackend.payload.request.LoginRequest
import com.pokerleaguebackend.payload.response.LoginResponse
import com.pokerleaguebackend.payload.request.RegisterAndClaimRequest
import com.pokerleaguebackend.payload.response.ApiResponse
import com.pokerleaguebackend.service.PlayerAccountService
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.security.JwtTokenProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val playerAccountService: PlayerAccountService,
    private val tokenProvider: JwtTokenProvider,
    private val leagueService: LeagueService
) {

    @PostMapping("/signin")
    fun authenticateUser(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.email,
                    loginRequest.password
                )
            )

            SecurityContextHolder.getContext().authentication = authentication

            val jwt = tokenProvider.generateToken(authentication)
            val userPrincipal = authentication.principal as com.pokerleaguebackend.security.UserPrincipal
            val playerAccount = userPrincipal.playerAccount

            return ResponseEntity.ok(LoginResponse(jwt, playerAccount.id, playerAccount.firstName, playerAccount.lastName, playerAccount.email))
        } catch (ex: Exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Invalid email or password"))
        }
    }

    @PostMapping("/signup")
    fun registerUser(@Valid @RequestBody signUpRequest: SignUpRequest): ResponseEntity<ApiResponse> {
        return try {
            val result = playerAccountService.registerUser(signUpRequest)

            val location: URI = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{email}")
                .buildAndExpand(result.email).toUri()

            ResponseEntity.created(location).body(ApiResponse(true, "User registered successfully!"))
        } catch (e: com.pokerleaguebackend.exception.DuplicatePlayerException) {
            ResponseEntity(ApiResponse(false, e.message ?: "An error occurred"), HttpStatus.BAD_REQUEST)
        }
    }

    @PostMapping("/register-and-claim")
    fun registerAndClaim(@Valid @RequestBody registerAndClaimRequest: RegisterAndClaimRequest): ResponseEntity<ApiResponse> {
        return try {
            val result = playerAccountService.registerAndClaim(registerAndClaimRequest)

            val location: URI = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{email}")
                .buildAndExpand(result.email).toUri()

            ResponseEntity.created(location).body(ApiResponse(true, "User registered and profile claimed successfully!"))
        } catch (e: com.pokerleaguebackend.exception.DuplicatePlayerException) {
            ResponseEntity(ApiResponse(false, e.message ?: "An error occurred"), HttpStatus.BAD_REQUEST)
        } catch (e: com.pokerleaguebackend.exception.LeagueNotFoundException) {
            ResponseEntity(ApiResponse(false, e.message ?: "An error occurred"), HttpStatus.NOT_FOUND)
        } catch (e: IllegalStateException) {
            ResponseEntity(ApiResponse(false, e.message ?: "An error occurred"), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(ApiResponse(false, e.message ?: "An error occurred"), HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/invite-details/{token}")
    fun getInviteDetails(@PathVariable token: String): ResponseEntity<PublicPlayerInviteDto> {
        return try {
            val inviteDetails = leagueService.getInviteDetailsByToken(token)
            ResponseEntity.ok(inviteDetails)
        } catch (e: LeagueNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }
    }
}
