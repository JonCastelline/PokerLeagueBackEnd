package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "blind_structures")
class BlindStructures (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "league_id")
    val leagueId: Long,

    val level: Int,

    @Column(name = "small_blind")
    val smallBlind: Int,

    @Column(name = "big_blind")
    val bigBlind: Int
)