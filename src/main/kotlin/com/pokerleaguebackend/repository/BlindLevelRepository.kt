package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.BlindLevel
import org.springframework.data.jpa.repository.JpaRepository

interface BlindLevelRepository : JpaRepository<BlindLevel, Long>
