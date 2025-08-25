package com.pokerleaguebackend.payload.response

import com.pokerleaguebackend.model.GameStatus
import com.pokerleaguebackend.payload.dto.BlindLevelDto

data class GameStateResponse(
    val gameId: Long,
    val gameStatus: GameStatus,
    val timer: TimerStateDto,
    val players: List<PlayerStateDto>,
    val settings: GameSettingsDto
)

data class TimerStateDto(
    val timerStartTime: Long?,
    val timeRemainingInMillis: Long?,
    val currentLevelIndex: Int?,
    val blindLevels: List<BlindLevelDto>
)

data class PlayerStateDto(
    val id: Long, // This is the LeagueMembershipId
    val displayName: String,
    val iconUrl: String?,
    val rank: Int?,
    val isPlaying: Boolean,
    val isEliminated: Boolean,
    val place: Int?,
    val kills: Int,
    val bounties: Int, // Number of bounties collected by this player
    val hasBounty: Boolean // True if this player has a bounty on their head
)

data class GameSettingsDto(
    val timerDurationMinutes: Int,
    val trackKills: Boolean,
    val trackBounties: Boolean,
    val warningSoundEnabled: Boolean,
    val warningSoundTimeSeconds: Int
)
