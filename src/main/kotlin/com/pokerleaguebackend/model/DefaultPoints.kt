package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "default_points")
class DefaultPoints (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "league_id")
    val leagueId: Long,

    @Column(name = "season_id")
    val seasonId: Long,

    @Column(name = "default_points")
    val defaultPoints: Int
)