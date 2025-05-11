package org.bronco.payments.controllers.api

import java.util.UUID

data class CreateCustomerResponse(
    val customerId: UUID?,
    val login: String,
    val email: String, //wont customerId and login be enough?
    val activeAccount: Boolean,
    val errorDescription: String?
)
