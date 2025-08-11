package com.pokerleaguebackend.security

import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    val playerAccount: PlayerAccount,
    private val memberships: List<LeagueMembership>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()
        memberships.forEach { membership ->
            // Grant authority based on the role in a specific league
            authorities.add(SimpleGrantedAuthority("ROLE_${membership.role}_${membership.league.id}"))
            if (membership.isOwner) {
                authorities.add(SimpleGrantedAuthority("ROLE_OWNER_${membership.league.id}"))
            }
        }
        // Add a general role for easier checking if the user is an admin of ANY league
        if (memberships.any { it.role == com.pokerleaguebackend.model.UserRole.ADMIN }) {
            authorities.add(SimpleGrantedAuthority(SecurityRole.ADMIN.authority))
        }
        if (memberships.any { it.isOwner }) {
            authorities.add(SimpleGrantedAuthority(SecurityRole.OWNER.authority))
        }
        return authorities
    }

    override fun getPassword(): String = playerAccount.password ?: ""

    override fun getUsername(): String = playerAccount.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}

