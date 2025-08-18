package com.pokerleaguebackend.model

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Entity
@Table(name = "league_membership")
data class LeagueMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "player_account_id", nullable = true)
    val playerAccount: PlayerAccount?,

    @ManyToOne
    @JoinColumn(name = "league_id")
    val league: League,

    var displayName: String? = null,
    var iconUrl: String? = null,
    @Enumerated(EnumType.STRING)
    var role: UserRole,
    var isOwner: Boolean = false,
    var isActive: Boolean = true
)