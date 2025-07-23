package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import java.math.BigDecimal

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
    var killPoints: BigDecimal = BigDecimal.ZERO,
    var bountyPoints: BigDecimal = BigDecimal.ZERO,
    var durationSeconds: Int = 1200,

    @Enumerated(EnumType.STRING)
    var bountyOnLeaderAbsenceRule: BountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,

    var enableAttendancePoints: Boolean = true,
    var attendancePoints: BigDecimal = BigDecimal.ONE,
    var startingStack: Int = 1500,

    @OneToMany(mappedBy = "leagueSettings", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    var blindLevels: MutableList<BlindLevel> = mutableListOf(),

    @OneToMany(mappedBy = "leagueSettings", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    var placePoints: MutableList<PlacePoint> = mutableListOf()
)