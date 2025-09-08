package org.bronco.payments.repositories.account

import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import java.util.*

interface AccountRepository {
    suspend fun createNewAccount(customerId: UUID): AccountData

    suspend fun getCustomerAccountsByStatus(customerId: UUID, statuses: Set<AccountStatus>): List<AccountData>
}