package com.pokerleaguebackend.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class RestExceptionHandler {

    data class ErrorDto(val message: String?)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleForbidden(ex: AccessDeniedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
                      .body(ErrorDto(ex.message))

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
                      .body(ErrorDto(ex.message))
}
