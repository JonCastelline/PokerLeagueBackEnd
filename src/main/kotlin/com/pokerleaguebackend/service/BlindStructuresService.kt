package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.BlindStructures
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.BlindStructuresRepository

@Service
class BlindStructuresService @Autowired constructor(private val blindStructuresRepository: BlindStructuresRepository) {

    fun createBlindStructure(blindStructure: BlindStructures) {
        blindStructuresRepository.save(blindStructure)
    }

    fun getBlindStructuresByLeagueId(leagueId: Long): List<BlindStructures> {
        return blindStructuresRepository.findAllByLeagueId(leagueId)
    }

    fun updateBlindStructure(blindStructure: BlindStructures): BlindStructures {
        return blindStructuresRepository.save(blindStructure)
    }

    fun deleteBlindStructure(id: Long) {
        blindStructuresRepository.deleteById(id)
    }

    fun createBlindStructures(blindStructures: List<BlindStructures>) {
        blindStructuresRepository.saveAll(blindStructures)
    }

    fun updateBlindStructures(blindStructures: List<BlindStructures>): List<BlindStructures> {
        val updatedBlindStructures = mutableListOf<BlindStructures>()

        for (blindStructure in blindStructures) {
            val updatedBlindStructure = blindStructuresRepository.save(blindStructure)
            updatedBlindStructures.add(updatedBlindStructure)
        }

        return updatedBlindStructures
    }
}
