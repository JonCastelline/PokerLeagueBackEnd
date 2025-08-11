package com.pokerleaguebackend.security

enum class SecurityRole(val authority: String) {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
    OWNER("ROLE_OWNER")
}
