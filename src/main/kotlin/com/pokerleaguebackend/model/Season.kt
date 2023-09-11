package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "season")
class Season (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "season_name")
    val seasonName: String,

    @Column(name = "start_date")
    val startDate: String,

    @Column(name = "end_date")
    val endDate: String,

    @Column(name = "league_id")
    val leagueId: Long
)