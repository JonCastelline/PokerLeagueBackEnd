package com.example.pokerleaguebackend

import jakarta.persistence.*

@Entity
@Table(name = "league_settings")
data class LeagueSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne
    @JoinColumn(name = "season_id", referencedColumnName = "id")
    val season: Season,

    val trackKills: Boolean = false,
    val trackBounties: Boolean = false,
    val killPoints: java.math.BigDecimal,
    val bountyPoints: java.math.BigDecimal,
    val durationSeconds: Int = 1200,
    val bountyOnLeaderAbsenceRule: String,
    val blindStructure: String,
    val placePoints: String
)