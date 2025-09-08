package org.bronco.payments.repositories.account.model

import jakarta.persistence.Column
import java.math.BigDecimal
import java.util.*

data class AccountData(
    @Column(name = "id")
    val accountId: UUID,
    @Column(name = "currency_code")
    val currencyCode: String,
    @Column(name = "balance")
    val balance: BigDecimal,
    @Column(name = "name")
    val accountName: String?,
    @Column(name = "status")
    val status: AccountStatus,
    @Column(name = "customer_reference")
    val customerId: UUID
)