package com.pokerleaguebackend.security

import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    private val playerAccount: PlayerAccount
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        if (playerAccount.admin) {
            authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
        } else {
            authorities.add(SimpleGrantedAuthority("ROLE_USER"))
        }
        return authorities
    }

    override fun getPassword(): String {
        return playerAccount.password
    }

    override fun getUsername(): String {
        return playerAccount.email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}