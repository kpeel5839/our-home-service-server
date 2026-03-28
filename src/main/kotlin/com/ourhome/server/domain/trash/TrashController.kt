package com.ourhome.server.domain.trash

import com.ourhome.server.config.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trash")
class TrashController(
    private val trashScheduleRepository: TrashScheduleRepository,
    private val trashRuleRepository: ApartmentTrashRuleRepository
) {

    @GetMapping("/rules")
    fun getRules(): ResponseEntity<List<ApartmentTrashRuleResponse>> =
        ResponseEntity.ok(trashRuleRepository.findAll().map { it.toResponse() })

    @GetMapping("/schedules")
    fun getSchedules(@RequestParam date: String): ResponseEntity<List<TrashScheduleResponse>> =
        ResponseEntity.ok(trashScheduleRepository.findByDate(date).map { it.toResponse() })

    @PostMapping("/schedules")
    fun createSchedule(@RequestBody request: CreateTrashScheduleRequest): ResponseEntity<TrashScheduleResponse> {
        val schedule = TrashSchedule(
            date = request.date,
            memberId = request.memberId,
            trashTypes = request.trashTypes.toMutableList()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(trashScheduleRepository.save(schedule).toResponse())
    }

    @PatchMapping("/schedules/{id}/complete")
    fun toggleComplete(@PathVariable id: String): ResponseEntity<TrashScheduleResponse> {
        val schedule = trashScheduleRepository.findById(id).orElseThrow { NotFoundException("Schedule not found: $id") }
        schedule.isCompleted = !schedule.isCompleted
        return ResponseEntity.ok(trashScheduleRepository.save(schedule).toResponse())
    }
}
