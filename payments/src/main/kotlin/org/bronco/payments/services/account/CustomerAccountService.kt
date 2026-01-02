package org.bronco.payments.services.account

import org.bronco.payments.model.Currencies
import org.bronco.payments.repositories.account.AccountRepository
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.repositories.progress.ProcessProgressProperties.Companion.ofInitial
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.account.model.AccountCreationData
import org.bronco.payments.services.account.model.CreateCustomerAccountCommand
import org.bronco.payments.services.kafka.producer.account.AccountKafkaProducer
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomerAccountService(
    private val accountRepository: AccountRepository,
    private val progressRepository: ProgressRepository,
    private val accountKafkaProducer: AccountKafkaProducer
) : AccountService {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private val notClosedAccountStatuses =
            setOf(AccountStatus.OPEN, AccountStatus.INACTIVE, AccountStatus.SUSPENDED, AccountStatus.BLOCKED)
        private val createdAccountStatuses = setOf(ProcessType.CREATE_CUSTOMER_ACCOUNT)
        private val successfulProcessStatusesStrings =
            setOf(ProgressType.FINISHED.name, ProgressType.ALREADY_PROCESSED.name)
    }

    override suspend fun createAccountForABrandNewCustomer(
        customerId: UUID,
        parentProcessId: UUID?
    ): List<AccountData> {
        val accounts = accountRepository.getCustomerAccountsByStatus(customerId, notClosedAccountStatuses)
        return accounts.ifEmpty {
            val properties = ofInitial(ProcessType.CREATE_CUSTOMER_ACCOUNT,  parentProcessId)
            progressRepository.initiateProgress(properties)
            return createNewAccount(
                AccountCreationData(
                    customerId = customerId,
                    processId = properties.id,
                    parentProcessId = parentProcessId,
                    currencyCode = Currencies.USD.name,
                )
            )
        }
    }

    override suspend fun createNewAccount(
        request: AccountCreationData
    ): List<AccountData> {
        val properties = ProcessProgressProperties.of(
            processId = request.processId,
            processType = ProcessType.CREATE_CUSTOMER_ACCOUNT,
            parentProcessId = request.parentProcessId,
            progressType = ProgressType.IN_PROGRESS,
        )
        progressRepository.updateProgress(
            properties
        )
        return runCatching { accountRepository.createNewAccount(request) }
            .fold(
                onSuccess = { accountData ->
                    finalizeAccountCreation(properties, accountData)
                },
                onFailure = { exception ->
                    handleAccountCreationFailure(exception, properties)
                }
            )
    }

    override suspend fun scheduleNewAccountCreation(
        parentProcessId: UUID,
        customerId: UUID,
        commands: List<CreateCustomerAccountCommand>
    ): List<ProcessProgressDetails> {
        return commands.map { request ->
            val processProgressProperties = ofInitial(ProcessType.CREATE_CUSTOMER_ACCOUNT, parentProcessId)
            progressRepository.initiateProgress(processProgressProperties)
            AccountCreationData(
                customerId = customerId,
                processId = processProgressProperties.id,
                parentProcessId = processProgressProperties.parentProcessId,
                currencyCode = request.currencyCode,
                accountName = request.accountName,
            )
        }
            .map { createCustomerAccount ->
                accountKafkaProducer.dispatchCreateCustomerAccount(createCustomerAccount)
            }
    }

    override suspend fun scheduleNewAccountCreation(
        parentProcessId: UUID,
        command: CreateCustomerAccountCommand,
    ): ProcessProgressDetails = scheduleNewAccountCreation(parentProcessId, command.customerId, listOf(command))
        .first()

    private suspend fun handleAccountCreationFailure(
        exception: Throwable,
        processProgressProperties: ProcessProgressProperties
    ): List<AccountData> {
        logger.error("An error has occurred while creating a new account", exception)
        progressRepository.updateProgress(
            processProgressProperties.copy(
                progressType = ProgressType.FINISHED_WITH_ERROR,
                progressDetails = exception.message
            ),
        )
        throw exception
    }

    override suspend fun getAccountsByCustomerId(customerId: UUID): List<AccountData> =
        accountRepository.getAllCustomerAccounts(customerId)

    override suspend fun getById(customerId: UUID): AccountData? = accountRepository.getById(customerId)

    override suspend fun getAccountsByProcessId(processId: UUID): List<AccountData> {
        val accountIds: Set<UUID> = getAccountIdsForProcesses(
            progressRepository.findProcessDetailsForProcessNamesByIds(
                createdAccountStatuses,
                processId = processId
            )
        )
        return accountRepository.getByIds(accountIds)
    }

    override suspend fun getAccountsByParentProcessId(parentProcessId: UUID): List<AccountData> {
        val accountIds: Set<UUID> = getAccountIdsForProcesses(
            progressRepository.findProcessDetailsForProcessNamesByIds(
                createdAccountStatuses,
                parentProcessId = parentProcessId
            )
        )
        return accountRepository.getByIds(accountIds)
    }

    private fun getAccountIdsForProcesses(processes: List<ProcessProgressDetails>): Set<UUID> {
        val accountIds: Set<UUID> = processes.filter { process ->
            process.progress in successfulProcessStatusesStrings
        }.mapNotNull { process -> process.entityId }.toSet()
        return accountIds
    }

    private suspend fun finalizeAccountCreation(
        processProgressProperties: ProcessProgressProperties,
        newAccount: AccountData,
    ): List<AccountData> {
        logger.info("An account was successfully created for customer: $processProgressProperties")
        progressRepository.updateProgress(
            processProgressProperties.copy(progressType = ProgressType.FINISHED, entityId = newAccount.accountId)
        )
        return listOf(newAccount)
    }
}