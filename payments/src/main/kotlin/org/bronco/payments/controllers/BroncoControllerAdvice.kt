package org.bronco.payments.controllers

import org.bronco.payments.controllers.api.ErrorDetail
import org.bronco.payments.controllers.api.ErrorTypes
import org.bronco.payments.controllers.api.PaymentsErrorResponse
import org.bronco.payments.model.ResourceNotFoundException
import org.springframework.context.MessageSourceResolvable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException


@RestControllerAdvice
class BroncoControllerAdvice {
    //TODO: most probably it's useless now, to be checked
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidationException(exception: HandlerMethodValidationException): ResponseEntity<PaymentsErrorResponse> {
        val results = exception.allErrors.map { error: MessageSourceResolvable ->
            when (error) {
                is FieldError -> ErrorDetail(
                    error.rejectedValue?.toString(),
                    error.field,
                    error.defaultMessage
                )

                is ObjectError -> ErrorDetail(error.objectName, null, error.defaultMessage)
                else -> ErrorDetail(null, null, error.defaultMessage)
            }
        }
        val badRequest = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(badRequest).body(
            PaymentsErrorResponse(
                code = badRequest.value(),
                status = badRequest.name,
                type = ErrorTypes.VALIDATION,
                details = results
            )
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(exception: WebExchangeBindException): ResponseEntity<PaymentsErrorResponse> {
        val results = exception.allErrors.map { error: MessageSourceResolvable ->
            when (error) {
                is FieldError -> ErrorDetail(
                    error.rejectedValue?.toString(),
                    error.field,
                    error.defaultMessage
                )

                is ObjectError -> ErrorDetail(error.objectName, null, error.defaultMessage)
                else -> ErrorDetail(null, null, error.defaultMessage)
            }
        }
        val badRequest = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(badRequest).body(
            PaymentsErrorResponse(
                code = badRequest.value(),
                status = badRequest.name,
                type = ErrorTypes.VALIDATION,
                details = results
            )
        )
    }

    //TODO: most probably it's useless now, to be checked
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodValidationException(exception: MethodArgumentNotValidException): ResponseEntity<PaymentsErrorResponse> {
        val violationDetails = exception.allErrors.map { error: ObjectError ->
            when (error) {
                is FieldError -> ErrorDetail(
                    error.rejectedValue?.toString(),
                    error.field,
                    error.defaultMessage
                )

                else -> ErrorDetail(error.objectName, null, error.defaultMessage)
            }
        }

        val badRequest = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(badRequest).body(
            PaymentsErrorResponse(
                code = badRequest.value(),
                status = badRequest.name,
                type = ErrorTypes.VALIDATION,
                details = violationDetails
            )
        )
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(exception: ResourceNotFoundException): ResponseEntity<PaymentsErrorResponse> {
        val notFound = HttpStatus.NOT_FOUND
        return ResponseEntity.status(notFound)
            .body(
                PaymentsErrorResponse(
                    notFound.value(),
                    notFound.name,
                    ErrorTypes.RESOURCE_NOT_FOUND,
                    listOf(ErrorDetail(null, null, exception.message))
                )
            )
    }
}