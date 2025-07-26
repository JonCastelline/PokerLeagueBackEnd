
package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.payload.LoginRequest
import com.pokerleaguebackend.payload.SignUpRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should register a new user and then log in`() {
        val uniqueEmail = "authcontrollerintegrationtest-test.user" + System.currentTimeMillis() + "@example.com"
        val signUpRequest = SignUpRequest(
            firstName = "Test",
            lastName = "User",
            email = uniqueEmail,
            password = "password"
        )

        // Sign up
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(signUpRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.message") { value("User registered successfully!") }
        }

        val loginRequest = LoginRequest(
            email = uniqueEmail,
            password = "password"
        )

        // Sign in
        mockMvc.post("/api/auth/signin") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
        }
    }
}
