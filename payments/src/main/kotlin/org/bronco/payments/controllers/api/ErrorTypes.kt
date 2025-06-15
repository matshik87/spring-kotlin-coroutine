package org.bronco.payments.controllers.api

enum class ErrorTypes(private val message: String) {
    UNKNOWN("Unknown"),
    VALIDATION("Validation error"),
    RESOURCE_NOT_FOUND("Resource not found"), ;

    override fun toString(): String = message
}