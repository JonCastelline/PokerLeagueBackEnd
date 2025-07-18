package com.example.pokerleaguebackend

import jakarta.persistence.*
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

    @ManyToOne
    @JoinColumn(name = "league_id")
    val league: League
)