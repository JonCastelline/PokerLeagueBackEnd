package com.pokerleaguebackend.payload.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class InvitePlayerRequest(
    @field:NotBlank
    @field:Email
    val email: String
)