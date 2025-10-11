package org.bronco.payments.services.account

import org.bronco.payments.repositories.account.AccountRepository
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.impl.ProcessProgressRepository
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProgressType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomerAccountService(
    private val accountRepository: AccountRepository,
    private val processProgressRepository: ProcessProgressRepository
) : AccountService {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private val notClosedAccountStatuses =
            setOf(AccountStatus.OPEN, AccountStatus.INACTIVE, AccountStatus.SUSPENDED, AccountStatus.BLOCKED)
    }

    override suspend fun createNewAccountOrRetrieveAllExistingAccounts(customerId: UUID): List<AccountData> {
        val accounts = accountRepository.getCustomerAccountsByStatus(customerId, notClosedAccountStatuses)
        return if (accounts.isEmpty()) {
            val processId = UUID.randomUUID()
            val progressKey = ProgressKey(processId, ProcessName.CREATE_CUSTOMER_ACCOUNT)
            processProgressRepository.initiateProgress(progressKey, id = null)
            val result = runCatching { accountRepository.createNewAccount(customerId) }
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                logger.error("An error has occurred while creating a new account", exception)
                processProgressRepository.updateProgress(
                    progressKey,
                    ProgressType.FINISHED_WITH_ERROR,
                    null,
                    exception?.message
                )
                throw exception!!
            } else {
                logger.info("An account was successfully created for $customerId")
                val newAccount = result.getOrNull()
                processProgressRepository.updateProgress(
                    progressKey,
                    ProgressType.FINISHED,
                    newAccount?.accountId,
                    null
                )
                listOf(newAccount!!)
            }
        } else {
            accounts
        }
    }

    override suspend fun getAccountsByCustomerId(customerId: UUID): List<AccountData> = accountRepository.getAllCustomerAccounts(customerId)
}