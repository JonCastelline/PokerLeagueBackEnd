package com.pokerleaguebackend.model

import jakarta.persistence.*
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
