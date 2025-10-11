package org.bronco.payments.controllers.api

import org.bronco.payments.repositories.account.model.AccountDto
import java.util.UUID

data class CreateCustomerResponse(
    val customerId: UUID?,
    val login: String,
    val email: String,
    val activeAccount: Boolean,
    val requiresPasswordChange: Boolean,
    val errorDescription: String?,
    val accounts: List<AccountDto> = emptyList()
)
