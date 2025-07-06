package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerAccountRepository :
    JpaRepository<PlayerAccount?, Long?> {
        fun findByEmail(email: String): PlayerAccount?
}