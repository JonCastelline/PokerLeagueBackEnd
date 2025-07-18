package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.util.UUID

@Entity
@Table(name = "league")
data class League(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val leagueName: String,
    val inviteCode: String = UUID.randomUUID().toString()
)