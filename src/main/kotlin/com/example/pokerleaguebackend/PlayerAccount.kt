package com.example.pokerleaguebackend

import jakarta.persistence.*

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