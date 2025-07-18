
package com.example.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.example.pokerleaguebackend.payload.LoginRequest
import com.example.pokerleaguebackend.payload.SignUpRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should register a new user and then log in`() {
        val signUpRequest = SignUpRequest(
            firstName = "Test",
            lastName = "User",
            email = "test.user@example.com",
            password = "password"
        )

        // Sign up
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(signUpRequest)
        }.andExpect {
            status { isCreated() }
            content { string("User registered successfully!") }
        }

        val loginRequest = LoginRequest(
            email = "test.user@example.com",
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
