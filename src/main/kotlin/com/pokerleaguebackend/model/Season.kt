package com.pokerleaguebackend.model

import jakarta.persistence.Column
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

    var seasonName: String,
    @Column(columnDefinition = "DATE")
    var startDate: Date,
    @Column(columnDefinition = "DATE")
    var endDate: Date,
    var isFinalized: Boolean = false,
    var isCasual: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "league_id")
    var league: League
)