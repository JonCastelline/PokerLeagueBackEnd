package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonManagedReference
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

    @OneToMany(mappedBy = "leagueSettings", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    val blindLevels: List<BlindLevel> = emptyList(),

    @OneToMany(mappedBy = "leagueSettings", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    val placePoints: List<PlacePoint> = emptyList()
)