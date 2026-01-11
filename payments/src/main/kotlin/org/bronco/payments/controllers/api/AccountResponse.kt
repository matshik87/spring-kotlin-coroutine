package org.bronco.payments.controllers.api

import org.bronco.payments.repositories.account.model.AccountStatus
import java.math.BigDecimal
import java.util.*

data class AccountResponse(
    val accountId: UUID,
    val customerId: UUID,
    val currencyCode: String,
    val balance: BigDecimal,
    val accountName: String?,
    val status: AccountStatus
)