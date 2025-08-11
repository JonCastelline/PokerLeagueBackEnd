package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(name = "season")
data class Season(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val seasonName: String,
    val startDate: Date,
    val endDate: Date,
    var isFinalized: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "league_id")
    var league: League
)