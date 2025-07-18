package com.example.pokerleaguebackend

import com.example.pokerleaguebackend.model.League

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "league_membership")
data class LeagueMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "player_account_id")
    val playerAccount: PlayerAccount,

    @ManyToOne
    @JoinColumn(name = "league_id")
    val league: League,

    val playerName: String,
    val role: String
)