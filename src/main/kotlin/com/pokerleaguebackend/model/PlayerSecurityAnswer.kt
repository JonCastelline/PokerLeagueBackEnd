package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Column
import jakarta.persistence.Table
import jakarta.persistence.ManyToOne
import jakarta.persistence.JoinColumn

@Entity
@Table(name = "player_security_answer")
data class PlayerSecurityAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "player_account_id", nullable = false)
    val playerAccount: PlayerAccount,

    @ManyToOne
    @JoinColumn(name = "security_question_id", nullable = false)
    val securityQuestion: SecurityQuestion,

    @Column(nullable = false)
    var hashedAnswer: String
)
