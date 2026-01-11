package org.bronco.payments.services.account

import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.services.account.model.AccountCreationData
import org.bronco.payments.services.account.model.CreateCustomerAccountCommand
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import java.util.*

interface AccountService {
    suspend fun scheduleNewAccountCreation(parentProcessId: UUID, customerId: UUID, commands: List<CreateCustomerAccountCommand>): List<ProcessProgressDetails>

    suspend fun createAccountForABrandNewCustomer(customerId: UUID, parentProcessId: UUID?): List<AccountData>
    suspend fun createNewAccount(request: AccountCreationData): List<AccountData>
    suspend fun scheduleNewAccountCreation(parentProcessId: UUID, command: CreateCustomerAccountCommand): ProcessProgressDetails

    suspend fun getAccountsByCustomerId(customerId: UUID): List<AccountData>

    suspend fun getById(customerId: UUID): AccountData?
    suspend fun getAccountsByProcessId(processId: UUID): List<AccountData>
    suspend fun getAccountsByParentProcessId(parentProcessId: UUID): List<AccountData>
}