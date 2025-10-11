package org.bronco.payments.services.account

import org.bronco.payments.repositories.account.model.AccountData
import java.util.*

interface AccountService {
    suspend fun createNewAccountOrRetrieveAllExistingAccounts(customerId: UUID): List<AccountData>

    suspend fun getAccountsByCustomerId(customerId: UUID): List<AccountData>
}