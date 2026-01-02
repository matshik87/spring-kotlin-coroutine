package org.bronco.payments.services.account.model

import java.util.*

data class CreateCustomerAccountCommand(
    val customerId: UUID,
    val currencyCode: String,
    val accountName: String?,
)

data class AccountCreationData(
    val customerId: UUID,
    val processId: UUID,
    val currencyCode: String,
    val parentProcessId: UUID? = null,
    val accountName: String? = null,
)