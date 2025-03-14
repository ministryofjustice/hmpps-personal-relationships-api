package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.exception.DuplicateEmailException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.exception.InvalidReferenceCodeGroupException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate.DuplicatePersonException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.format.DateTimeParseException

@RestControllerAdvice
class HmppsContactsApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Entity not found : ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Entity not found exception: {}", e.message) }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(
      ErrorResponse(
        status = HttpStatus.FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.debug("Forbidden (403) returned: {}", e.message) }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleInvalidReferenceCodeGroupException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    var message = e.message
    val rootCause = ExceptionUtils.getRootCause(e)
    if (rootCause != null && rootCause is InvalidReferenceCodeGroupException) {
      message = rootCause.message
    }
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Request parameters are invalid",
          developerMessage = message,
        ),
      ).also { log.info("Failed to parse request parameters: {}", e.message) }
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    val cause = e.cause
    val message = if (cause is MismatchedInputException) {
      "Validation failure: ${sanitiseMismatchInputException(cause)}"
    } else {
      "Validation failure: Couldn't read request body"
    }
    return ResponseEntity.status(BAD_REQUEST).body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = message,
        developerMessage = e.message,
      ),
    ).also { log.error(message, e) }
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure(s): ${
          e.allErrors.map { formatMessage(it) }.distinct().sorted().joinToString(System.lineSeparator())
        }",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(DuplicatePersonException::class, DuplicateEmailException::class)
  fun handleDuplicateException(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(CONFLICT)
    .body(
      ErrorResponse(
        status = CONFLICT,
        userMessage = e.message,
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleValidationException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  private fun sanitiseMismatchInputException(cause: MismatchedInputException): String {
    val name = cause.path.fold("") { jsonPath, ref ->
      val suffix = when {
        ref.index > -1 -> "[${ref.index}]"
        else -> ".${ref.fieldName}"
      }
      (jsonPath + suffix).removePrefix(".")
    }
    val problem = when (cause.cause) {
      is DateTimeParseException -> "could not be parsed as a date"
      else -> "must not be null"
    }
    return "$name $problem"
  }

  private fun formatMessage(error: ObjectError): String {
    var message = error.defaultMessage ?: ""
    if (error is FieldError) {
      val lastPathPart = error.field.substringAfterLast(".")
      if (message.startsWith(lastPathPart)) {
        message = error.field.substring(0, error.field.length - lastPathPart.length) + message
      } else {
        message = error.field + " " + message
      }
    }
    return message
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
