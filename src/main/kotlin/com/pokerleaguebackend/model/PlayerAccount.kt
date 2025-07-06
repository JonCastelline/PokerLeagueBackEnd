package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Column
import jakarta.persistence.OneToMany
import jakarta.persistence.CascadeType
import java.util.ArrayList

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

    @OneToMany(mappedBy = "playerAccount", cascade = [CascadeType.ALL], orphanRemoval = true)
    val players: List<Player> = ArrayList()
)
