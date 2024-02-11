package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "player_account")
data class PlayerAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String,

    @Column(unique = true)
    val email: String,

    val password: String,

    val paid: Boolean = false,

    val admin: Boolean = false,

    @Column(name = "super_admin")
    val superAdmin: Boolean = false,

    @Column(name = "default_league_id")
    val defaultLeagueId: Long? = null
)
