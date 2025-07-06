package com.pokerleaguebackend.model

import jakarta.persistence.*

@Entity
@Table(name = "league")
data class League(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val leagueName: String
)