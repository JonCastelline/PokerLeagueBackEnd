package com.pokerleaguebackend.security

enum class SecurityRole(val authority: String) {
    ADMIN("ROLE_ADMIN"),
    OWNER("ROLE_OWNER")
}
