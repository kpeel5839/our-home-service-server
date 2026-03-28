package com.ourhome.server.config

import com.ourhome.server.common.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class NotFoundException(message: String) : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message ?: "Not found", "NOT_FOUND"))

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(e: ConflictException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(e.message ?: "Conflict", "CONFLICT"))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: "Bad request", "BAD_REQUEST"))
}
