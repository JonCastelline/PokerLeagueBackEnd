package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "player_account")
data class PlayerAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var firstName: String,
    var lastName: String,

    @Column(unique = true)
    var email: String,

    @Column(length = 100)
    var password: String? = null,
    val paid: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "last_league_id")
    var lastLeague: League? = null
)