package com.example.pokerleaguebackend

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

    val firstName: String,
    val lastName: String,

    @Column(unique = true)
    val email: String,

    val password: String,
    val paid: Boolean = false
)