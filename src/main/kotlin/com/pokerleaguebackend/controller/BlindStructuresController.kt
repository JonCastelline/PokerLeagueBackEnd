package com.pokerleaguebackend.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import com.pokerleaguebackend.service.BlindStructuresService
import com.pokerleaguebackend.model.BlindStructures

@RestController
@RequestMapping("/api/blindStructures")
class BlindStructuresController @Autowired constructor(private val blindStructuresService: BlindStructuresService) {

    @GetMapping("/{leagueId}")
    fun getBlindStructuresByLeagueId(@PathVariable leagueId: Long): List<BlindStructures> {
        return blindStructuresService.getBlindStructuresByLeagueId(leagueId)
    }

    @PostMapping("/createSingle")
    fun createBlindStructure(@RequestBody blindStructure: BlindStructures) {
        blindStructuresService.createBlindStructure(blindStructure)
    }

    @PostMapping("/createMultiple")
    fun createBlindStructures(@RequestBody blindStructures: List<BlindStructures>) {
        blindStructuresService.createBlindStructures(blindStructures)
    }

    @PutMapping("/{id}")
    fun updateBlindStructure(@PathVariable id: Long, @RequestBody blindStructure: BlindStructures): BlindStructures {
        // Ensure the ID in the request body matches the path variable
        if (blindStructure.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return blindStructuresService.updateBlindStructure(blindStructure)
    }

    @PutMapping
    fun updateBlindStructures(@RequestBody blindStructures: List<BlindStructures>): List<BlindStructures> {
        return blindStructuresService.updateBlindStructures(blindStructures)
    }

    @DeleteMapping("/{id}")
    fun deleteBlindStructure(@PathVariable id: Long) {
        blindStructuresService.deleteBlindStructure(id)
    }
}