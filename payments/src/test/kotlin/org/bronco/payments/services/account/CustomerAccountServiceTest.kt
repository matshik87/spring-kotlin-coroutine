package org.bronco.payments.services.account

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bronco.payments.model.Currencies
import org.bronco.payments.repositories.account.AccountRepository
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.progress.impl.ProcessProgressRepository
import org.bronco.payments.services.account.model.AccountCreationData
import org.bronco.payments.services.account.model.CreateCustomerAccountCommand
import org.bronco.payments.services.kafka.producer.account.AccountKafkaProducer
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.AssertionUtils.assertProgressProperties
import org.bronco.payments.utils.ProcessProgressGenerators.generateProcessProgressDetails
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class CustomerAccountServiceTest {
    @MockK
    private lateinit var accountRepository: AccountRepository

    @MockK(relaxed = true)
    private lateinit var processProgressRepository: ProcessProgressRepository

    @MockK
    private lateinit var accountData: AccountData

    @MockK
    private lateinit var accountKafkaProducer: AccountKafkaProducer

    @InjectMockKs
    private lateinit var customerAccountService: CustomerAccountService

    @Test
    fun createNewAccountForNewCustomer_whenNoAccountForCustomer_thenCreateAndReturnNewAccount() = runTest {
        val processId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns emptyList()
        coEvery { accountRepository.createNewAccount(any(AccountCreationData::class)) } returns accountData
        coEvery { accountData.accountId } returns accountId

        val accounts = customerAccountService.createAccountForABrandNewCustomer(customerId, processId)

        assertThat(accounts).contains(accountData)

        coVerify {
            processProgressRepository.initiateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.INITIALIZED,
                    processId,
                    null,
                )
            )
        }
        coVerify {
            processProgressRepository.updateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.FINISHED,
                    processId,
                    entityId = accountId,
                )
            )
        }
    }

    @Test
    fun createAccount_whenNoAccountForCustomerWithCustomProcessId_thenCreateAndReturnNewAccount() = runTest {
        val customerId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val processId = UUID.randomUUID()

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns emptyList()
        coEvery { accountRepository.createNewAccount(any(AccountCreationData::class)) } returns accountData
        coEvery { accountData.accountId } returns accountId

        val accounts = customerAccountService.createAccountForABrandNewCustomer(customerId, processId)

        assertThat(accounts).contains(accountData)

        coVerify {
            processProgressRepository.initiateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.INITIALIZED,
                    processId,
                    null,
                )
            )
        }
        coVerify {
            processProgressRepository.updateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.FINISHED,
                    processId,
                    accountId,
                )
            )
        }
    }

    @Test
    fun createAccount_whenCustomerHasAccount_thenItsReturned() = runTest {
        val processId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns listOf(accountData)

        coEvery { accountData.accountId } returns accountId

        val accounts = customerAccountService.createAccountForABrandNewCustomer(customerId, processId)

        assertThat(accounts).contains(accountData)

        coVerify(exactly = 0) {
            processProgressRepository.initiateProgress(any())
        }
        coVerify(exactly = 0) { accountRepository.createNewAccount(customerId) }
    }

    @Test
    fun createAccount_whenCreatingAccountHasFailed_thenExceptionIsThrown() = runTest {
        val processId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val expectedException = RuntimeException("sth went so wrong")

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns emptyList()
        coEvery { accountRepository.createNewAccount(any(AccountCreationData::class)) } throws expectedException

        assertThatThrownBy {
            runBlocking {
                customerAccountService.createAccountForABrandNewCustomer(
                    customerId, processId
                )
            }
        }

        coVerify {
            processProgressRepository.initiateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.INITIALIZED,
                    processId,
                    null,
                )
            )
        }
        coVerify {
            processProgressRepository.updateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.FINISHED_WITH_ERROR,
                    processId,
                    null,
                    expectedException.message
                )
            )
        }
    }

    @Test
    fun getAccountsByCustomerId_whenCustomerHasAccounts_thenItsReturned() = runTest {
        val customerId = UUID.randomUUID()

        coEvery { accountRepository.getAllCustomerAccounts(customerId) } returns listOf(accountData)

        val accounts = customerAccountService.getAccountsByCustomerId(customerId)

        assertThat(accounts).singleElement()
            .isEqualTo(accountData)
    }

    @Test
    fun getAccountsByCustomerId_whenCustomerHasNoAccount_thenEmptyResponseIsReturned() = runTest {
        val customerId = UUID.randomUUID()

        coEvery { accountRepository.getAllCustomerAccounts(customerId) } returns emptyList()

        val accounts = customerAccountService.getAccountsByCustomerId(customerId)

        assertThat(accounts).isEmpty()
    }

    @Test
    fun getById_whenAccountWasFound_thenItsReturned() = runTest {
        val accountId = UUID.randomUUID()

        coEvery { accountRepository.getById(accountId) } returns accountData

        val accounts = customerAccountService.getById(accountId)

        assertThat(accounts).isEqualTo(accountData)
    }

    @Test
    fun getById_whenCustomerHasNoAccount_thenNullIsReturned() = runTest {
        val accountId = UUID.randomUUID()

        coEvery { accountRepository.getById(accountId) } returns null

        val accounts = customerAccountService.getById(accountId)

        assertThat(accounts).isNull()
    }

    @Test
    fun getAccountsByProcessId_whenSuccessfulAccountWereFound_thenTheyAreReturned() = runTest {
        val processId = UUID.randomUUID()
        val parentProcessId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessTypesByIds(any(), processId, isNull()) } returns listOf(
            generateProcessProgressDetails(
                processId,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.ALREADY_PROCESSED,
                accountId,
                null
            ),
            generateProcessProgressDetails(
                processId,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED,
                accountId,
                null
            ),
            generateProcessProgressDetails(
                processId,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED_WITH_ERROR,
                UUID.randomUUID(),
                null
            )
        )
        coEvery { accountRepository.getByIds(any()) } returns listOf(accountData)

        val accounts = customerAccountService.getAccountsByProcessId(processId)

        assertThat(accounts).singleElement().isEqualTo(accountData)
        coVerify {
            accountRepository.getByIds(coWithArg { list ->
                assertThat(list).singleElement().isEqualTo(accountId)
            })
        }
    }

    @Test
    fun getAccountsByProcessId_whenNoProcessWasFound_thenEmptyListIsReturned() = runTest {
        val processId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessTypesByIds(any(), processId, isNull()) } returns emptyList()
        coEvery { accountRepository.getByIds(any()) } returns emptyList()

        val accounts = customerAccountService.getAccountsByProcessId(processId)

        assertThat(accounts).isEmpty()
        coEvery { accountRepository.getByIds(emptyList()) }
    }

    @Test
    fun getAccountsForProcessId_whenOnlyAccountsWithNotSuccessfulStatusWereFound_thenEmptyListIsReturned() = runTest {
        val processId = UUID.randomUUID()
        val parentProcessId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessTypesByIds(any(), processId, isNull()) } returns listOf(
            generateProcessProgressDetails(
                processId,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.IN_PROGRESS,
                UUID.randomUUID(),
                null
            ),
            generateProcessProgressDetails(
                processId,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.INITIALIZED,
                UUID.randomUUID(),
                null
            ),
            generateProcessProgressDetails(
                processId,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED_WITH_ERROR,
                UUID.randomUUID(),
                null
            )
        )
        coEvery { accountRepository.getByIds(any()) } returns emptyList()

        val accounts = customerAccountService.getAccountsByProcessId(processId)

        assertThat(accounts).isEmpty()
        coEvery { accountRepository.getByIds(emptyList()) }
    }

    @Test
    fun scheduleNewAccountCreation_whenActionWasScheduled_thenProgressDetailsAreReturned() = runTest {
        val parentProcessId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val command = CreateCustomerAccountCommand(customerId, Currencies.USD.name, null)

        coEvery { accountKafkaProducer.dispatchCreateCustomerAccount(any()) } answers {
            val payload = firstArg<AccountCreationData>()
            ProcessProgressDetails(
                id = payload.processId,
                type = ProcessType.CREATE_CUSTOMER_ACCOUNT.name,
                parentId = payload.parentProcessId,
                progress = ProgressType.DISPATCHED.name,
                entityId = null,
                details = null
            )
        }

        val result = customerAccountService.scheduleNewAccountCreation(parentProcessId, command)

        assertThat(result)
            .returns(parentProcessId) { it.parentId }
            .returns(ProcessType.CREATE_CUSTOMER_ACCOUNT.name) { it.type }
            .returns(ProgressType.DISPATCHED.name) { it.progress }
            .returns(null) { it.details }
            .returns(null) { it.entityId }
        assertThat(result.id).isNotNull()

        coVerify {
            processProgressRepository.initiateProgress(
                assertProgressProperties(
                    ProcessType.CREATE_CUSTOMER_ACCOUNT,
                    ProgressType.INITIALIZED,
                    parentProcessId,
                    null,
                )
            )
        }
    }

    @Test
    fun `getAccountsByParentProcessId for existing processes the actual processes are retrieved`() = runTest {
        val process1 = UUID.randomUUID()
        val process2 = UUID.randomUUID()
        val parentProcessId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessTypesByIds(any(), isNull(), parentProcessId) } returns listOf(
            generateProcessProgressDetails(
                process1,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.ALREADY_PROCESSED,
                accountId,
                null
            ),
            generateProcessProgressDetails(
                process1,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED,
                accountId,
                null
            ),
            generateProcessProgressDetails(
                process2,
                parentProcessId,
                ProcessType.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED_WITH_ERROR,
                UUID.randomUUID(),
                null
            )
        )
        coEvery { accountRepository.getByIds(any()) } returns listOf(accountData)

        val accounts = customerAccountService.getAccountsByParentProcessId(parentProcessId)

        assertThat(accounts).singleElement().isEqualTo(accountData)
        coVerify {
            accountRepository.getByIds(coWithArg { list ->
                assertThat(list).singleElement().isEqualTo(accountId)
            })
        }
    }
}