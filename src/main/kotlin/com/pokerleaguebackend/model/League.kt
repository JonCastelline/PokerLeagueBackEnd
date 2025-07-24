package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class League(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    var leagueName: String? = null,
    var inviteCode: String,
    var expirationDate: java.util.Date? = null
)
