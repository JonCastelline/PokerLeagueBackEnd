package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*

@Entity
@Table(name = "league_settings")
data class LeagueSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @OneToOne
    @JoinColumn(name = "season_id", referencedColumnName = "id")
    var season: Season,

    var trackKills: Boolean = false,
    var trackBounties: Boolean = false,
    var killPoints: java.math.BigDecimal,
    var bountyPoints: java.math.BigDecimal,
    var durationSeconds: Int = 1200,
    var bountyOnLeaderAbsenceRule: String,

    @OneToMany(mappedBy = "leagueSettings", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    var blindLevels: MutableList<BlindLevel> = mutableListOf(),

    @OneToMany(mappedBy = "leagueSettings", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    var placePoints: MutableList<PlacePoint> = mutableListOf()
)