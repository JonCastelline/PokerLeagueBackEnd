package com.pokerleaguebackend.model

import com.pokerleaguebackend.model.enums.InviteStatus
import java.util.Date
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.ManyToOne
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Column
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType

@Entity
@Table(name = "player_invites")
class PlayerInvite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_membership_id", nullable = false)
    val leagueMembership: LeagueMembership,

    @Column(nullable = false)
    val email: String,

    @Column(unique = true, nullable = false)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InviteStatus = InviteStatus.PENDING,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    val expirationDate: Date
)