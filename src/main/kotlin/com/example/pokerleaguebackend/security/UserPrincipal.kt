package com.example.pokerleaguebackend.security

import com.example.pokerleaguebackend.PlayerAccount
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(private val playerAccount: PlayerAccount) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getPassword(): String = playerAccount.password

    override fun getUsername(): String = playerAccount.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
