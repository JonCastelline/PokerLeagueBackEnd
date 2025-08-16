package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "blind_levels")
data class BlindLevel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val level: Int,
    val smallBlind: Int,
    val bigBlind: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_settings_id")
    @JsonBackReference
    var seasonSettings: SeasonSettings? = null
)