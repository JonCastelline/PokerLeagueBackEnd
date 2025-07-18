package com.example.pokerleaguebackend.repository

import com.example.pokerleaguebackend.LeagueMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueMembershipRepository : JpaRepository<LeagueMembership, Long>