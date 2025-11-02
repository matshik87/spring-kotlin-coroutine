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
        private val createdAccountStatuses =  setOf(ProcessName.CREATE_CUSTOMER_ACCOUNT)
        private val successfulProcessStatusesStrings = setOf(ProgressType.FINISHED.name, ProgressType.ALREADY_PROCESSED.name)
    }

    override suspend fun createNewAccountOrRetrieveAllExistingAccounts(
        customerId: UUID,
        processId: UUID
    ): List<AccountData> {
        val accounts = accountRepository.getCustomerAccountsByStatus(customerId, notClosedAccountStatuses)
        return accounts.ifEmpty {
            val progressKey = ProgressKey(processId, ProcessName.CREATE_CUSTOMER_ACCOUNT)
            processProgressRepository.initiateProgress(progressKey, id = null)
            runCatching { accountRepository.createNewAccount(customerId) }
                .fold(
                    onSuccess = { accountData ->
                        finalizeAccountCreation(customerId, accountData, progressKey)
                    },
                    onFailure = { exception ->
                        handleAccountCreationFailure(exception, progressKey)
                    }
                )
        }
    }

    private suspend fun handleAccountCreationFailure(
        exception: Throwable,
        progressKey: ProgressKey
    ): List<AccountData> {
        logger.error("An error has occurred while creating a new account", exception)
        processProgressRepository.updateProgress(
            progressKey,
            ProgressType.FINISHED_WITH_ERROR,
            null,
            exception.message
        )
        throw exception
    }

    override suspend fun createNewAccountOrRetrieveAllExistingAccounts(customerId: UUID): List<AccountData> {
        return createNewAccountOrRetrieveAllExistingAccounts(customerId, UUID.randomUUID())
    }

    override suspend fun getAccountsByCustomerId(customerId: UUID): List<AccountData> = accountRepository.getAllCustomerAccounts(customerId)

    override suspend fun getById(customerId: UUID): AccountData? = accountRepository.getById(customerId)

    override suspend fun getAccountsForProcessId(processId: UUID): List<AccountData> {
        val processes = processProgressRepository.findProcessDetailsForProcessNames(processId, createdAccountStatuses)
        val accountIds: Set<UUID> = processes.filter { process ->
            process.progress in successfulProcessStatusesStrings
        }.mapNotNull { process -> process.entityId }.toSet()
        return accountRepository.getByIds(accountIds)
    }

    private suspend fun finalizeAccountCreation(
        customerId: UUID,
        newAccount: AccountData,
        progressKey: ProgressKey
    ): List<AccountData> {
        logger.info("An account was successfully created for customer: $customerId")
        processProgressRepository.updateProgress(
            progressKey,
            ProgressType.FINISHED,
            newAccount.accountId,
            null
        )
        return listOf(newAccount)
    }
}