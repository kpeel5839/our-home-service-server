package com.ourhome.server.domain.cleaning

import com.ourhome.server.config.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cleaning")
class CleaningController(
    private val taskRepository: CleaningTaskRepository,
    private val assignmentRepository: CleaningAssignmentRepository
) {

    @GetMapping("/tasks")
    fun getTasks(): ResponseEntity<List<CleaningTaskResponse>> =
        ResponseEntity.ok(taskRepository.findAll().map { it.toResponse() })

    @PostMapping("/tasks")
    fun createTask(@RequestBody request: CreateCleaningTaskRequest): ResponseEntity<CleaningTaskResponse> {
        val task = CleaningTask(
            name = request.name,
            frequency = request.frequency,
            dayOfWeek = request.dayOfWeek,
            assignedMemberId = request.assignedMemberId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(taskRepository.save(task).toResponse())
    }

    @PatchMapping("/tasks/{id}")
    fun updateTask(
        @PathVariable id: String,
        @RequestBody request: UpdateCleaningTaskRequest
    ): ResponseEntity<CleaningTaskResponse> {
        val task = taskRepository.findById(id).orElseThrow { NotFoundException("Task not found: $id") }
        request.name?.let { task.name = it }
        request.frequency?.let { task.frequency = it }
        request.dayOfWeek?.let { task.dayOfWeek = it }
        request.assignedMemberId?.let { task.assignedMemberId = it }
        return ResponseEntity.ok(taskRepository.save(task).toResponse())
    }

    @DeleteMapping("/tasks/{id}")
    fun deleteTask(@PathVariable id: String): ResponseEntity<Void> {
        if (!taskRepository.existsById(id)) throw NotFoundException("Task not found: $id")
        taskRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/assignments")
    fun getAssignments(@RequestParam date: String): ResponseEntity<List<CleaningAssignmentResponse>> =
        ResponseEntity.ok(assignmentRepository.findByScheduledDate(date).map { it.toResponse() })

    @PatchMapping("/assignments/{id}/complete")
    fun toggleComplete(@PathVariable id: String): ResponseEntity<CleaningAssignmentResponse> {
        val assignment = assignmentRepository.findById(id).orElseThrow { NotFoundException("Assignment not found: $id") }
        assignment.isCompleted = !assignment.isCompleted
        return ResponseEntity.ok(assignmentRepository.save(assignment).toResponse())
    }
}
