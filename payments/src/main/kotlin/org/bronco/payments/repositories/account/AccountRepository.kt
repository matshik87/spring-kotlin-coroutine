package org.bronco.payments.repositories.account

import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.services.account.model.AccountCreationData
import java.util.*

interface AccountRepository {
    suspend fun createNewAccount(customerId: UUID): AccountData
    suspend fun createNewAccount(request: AccountCreationData): AccountData

    suspend fun getById(accountId: UUID): AccountData?

    suspend fun getCustomerAccountsByStatus(customerId: UUID, statuses: Set<AccountStatus>): List<AccountData>

    suspend fun getAllCustomerAccounts(customerId: UUID): List<AccountData>

    suspend fun getByIds(ids: Collection<UUID>): List<AccountData>
}