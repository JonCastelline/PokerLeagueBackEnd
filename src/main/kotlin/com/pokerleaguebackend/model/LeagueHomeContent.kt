
package com.pokerleaguebackend.model

import jakarta.persistence.*

@Entity
@Table(name = "league_home_content")
data class LeagueHomeContent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", referencedColumnName = "id")
    val league: League,

    @Column(columnDefinition = "TEXT")
    var content: String
)
