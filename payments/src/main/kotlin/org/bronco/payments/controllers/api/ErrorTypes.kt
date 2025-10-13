package org.bronco.payments.controllers.api

enum class ErrorTypes(private val message: String) {
    UNKNOWN("Unknown"),
    VALIDATION("Validation error"),
    RESOURCE_NOT_FOUND("Resource not found"),
    RESOURCE_NOT_REMOVABLE("Resource can not be removed"), ;

    override fun toString(): String = message
}