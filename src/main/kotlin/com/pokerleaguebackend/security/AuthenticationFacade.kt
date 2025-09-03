package com.pokerleaguebackend.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationFacade {
    fun getAuthenticatedPlayerId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication.principal !is UserPrincipal) {
            throw IllegalStateException("User is not authenticated or principal is not UserPrincipal")
        }
        val userPrincipal = authentication.principal as UserPrincipal
        return userPrincipal.playerAccount.id
    }
}
