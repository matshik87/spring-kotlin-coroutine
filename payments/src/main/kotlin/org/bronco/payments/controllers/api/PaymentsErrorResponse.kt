package org.bronco.payments.controllers.api

class PaymentsErrorResponse(
    val code: Int,
    val status: String,
    val type: ErrorTypes?,
    val details: List<ErrorDetail>?
)

data class ErrorDetail(
    val value: String?,
    val field: String?,
    val message: String?
)