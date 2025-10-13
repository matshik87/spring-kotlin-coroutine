package org.bronco.payments.utils

import org.bronco.payments.model.Currencies
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import java.math.BigDecimal
import java.util.*

object AccountDataGenerators {
    fun generateAccount(
        accountId: UUID = UUID.randomUUID(),
        customerId: UUID = UUID.randomUUID(),
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        accountName: String? = null,
        accountStatus: AccountStatus = AccountStatus.OPEN,
    ): AccountData = AccountData(
        accountId = accountId,
        customerId = customerId,
        currencyCode = Currencies.USD.name,
        balance = balance,
        accountName = accountName,
        status = accountStatus
    )
}