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
import org.bronco.payments.repositories.account.AccountRepository
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.progress.impl.ProcessProgressRepository
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProgressType
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

    @InjectMockKs
    private lateinit var customerAccountService: CustomerAccountService

    @Test
    fun createAccount_whenNoAccountForCustomer_thenCreateAndReturnNewAccount() = runTest {
        val customerId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns emptyList()
        coEvery { accountRepository.createNewAccount(customerId) } returns accountData
        coEvery { accountData.accountId } returns accountId

        val accounts = customerAccountService.createNewAccountOrRetrieveAllExistingAccounts(customerId)

        assertThat(accounts).contains(accountData)

        coVerify {
            processProgressRepository.initiateProgress(coWithArg { progressKey ->
                assertThat(progressKey).isNotNull()
                assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                assertThat(progressKey.id).isNotNull()
            }, anyNullable())
        }
        coVerify {
            processProgressRepository.updateProgress(
                coWithArg { progressKey ->
                    assertThat(progressKey).isNotNull()
                    assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                    assertThat(progressKey.id).isNotNull()
                },
                ProgressType.FINISHED,
                coWithArg { accountId -> assertThat(accountId).isEqualTo(accountId) },
                anyNullable()
            )
        }
    }

    @Test
    fun createAccount_whenNoAccountForCustomerWithCustomProcessId_thenCreateAndReturnNewAccount() = runTest {
        val customerId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val processId = UUID.randomUUID()

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns emptyList()
        coEvery { accountRepository.createNewAccount(customerId) } returns accountData
        coEvery { accountData.accountId } returns accountId

        val accounts = customerAccountService.createNewAccountOrRetrieveAllExistingAccounts(customerId, processId)

        assertThat(accounts).contains(accountData)

        coVerify {
            processProgressRepository.initiateProgress(coWithArg { progressKey ->
                assertThat(progressKey).isNotNull()
                assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                assertThat(progressKey.id).isEqualTo(processId)
            }, anyNullable())
        }
        coVerify {
            processProgressRepository.updateProgress(
                coWithArg { progressKey ->
                    assertThat(progressKey).isNotNull()
                    assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                    assertThat(progressKey.id).isEqualTo(processId)
                },
                ProgressType.FINISHED,
                coWithArg { accountId -> assertThat(accountId).isEqualTo(accountId) },
                anyNullable()
            )
        }
    }

    @Test
    fun createAccount_whenCustomerHasAccount_thenItsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns listOf(accountData)

        coEvery { accountData.accountId } returns accountId

        val accounts = customerAccountService.createNewAccountOrRetrieveAllExistingAccounts(customerId)

        assertThat(accounts).contains(accountData)

        coVerify(exactly = 0) {
            processProgressRepository.initiateProgress(coWithArg { progressKey ->
                assertThat(progressKey).isNotNull()
                assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                assertThat(progressKey.id).isNotNull()
            }, anyNullable())
        }
        coVerify(exactly = 0) { accountRepository.createNewAccount(customerId) }
    }

    @Test
    fun createAccount_whenCreatingAccountHasFailed_thenExceptionIsThrown() = runTest {
        val customerId = UUID.randomUUID()
        val expectedException = RuntimeException("sth went so wrong")

        coEvery { accountRepository.getCustomerAccountsByStatus(customerId, any()) } returns emptyList()
        coEvery { accountRepository.createNewAccount(customerId) } throws expectedException

        assertThatThrownBy {
            runBlocking {
                customerAccountService.createNewAccountOrRetrieveAllExistingAccounts(
                    customerId
                )
            }
        }

        coVerify {
            processProgressRepository.initiateProgress(coWithArg { progressKey ->
                assertThat(progressKey).isNotNull()
                assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                assertThat(progressKey.id).isNotNull()
            }, anyNullable())
        }
        coVerify {
            processProgressRepository.updateProgress(
                coWithArg { progressKey ->
                    assertThat(progressKey).isNotNull()
                    assertThat(progressKey.name).isEqualTo(ProcessName.CREATE_CUSTOMER_ACCOUNT)
                    assertThat(progressKey.id).isNotNull()
                },
                ProgressType.FINISHED_WITH_ERROR,
                anyNullable(),
                expectedException.message
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
    fun getAccountsForProcessId_whenSuccessfulAccountWereFound_thenTheyAreReturned() = runTest {
        val processId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessNames(processId, any()) } returns listOf(
            generateProcessProgressDetails(
                processId,
                ProcessName.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.ALREADY_PROCESSED,
                accountId,
                null
            ),
            generateProcessProgressDetails(
                processId,
                ProcessName.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED,
                accountId,
                null
            ),
            generateProcessProgressDetails(
                processId,
                ProcessName.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED_WITH_ERROR,
                UUID.randomUUID(),
                null
            )
        )
        coEvery { accountRepository.getByIds(any()) } returns listOf(accountData)

        val accounts = customerAccountService.getAccountsForProcessId(processId)

        assertThat(accounts).singleElement().isEqualTo(accountData)
        coVerify { accountRepository.getByIds(coWithArg { list ->
            assertThat(list).singleElement().isEqualTo(accountId)
        }) }
    }

    @Test
    fun getAccountsForProcessId_whenNoProcessWasFound_thenEmptyListIsReturned() = runTest {
        val processId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessNames(processId, any()) } returns emptyList()
        coEvery { accountRepository.getByIds(any()) } returns emptyList()

        val accounts = customerAccountService.getAccountsForProcessId(processId)

        assertThat(accounts).isEmpty()
        coEvery { accountRepository.getByIds(emptyList()) }
    }

    @Test
    fun getAccountsForProcessId_whenOnlyAccountsWithNotSuccessfulStatusWereFound_thenEmptyListIsReturned() = runTest {
        val processId = UUID.randomUUID()

        coEvery { processProgressRepository.findProcessDetailsForProcessNames(processId, any()) } returns listOf(
            generateProcessProgressDetails(
                processId,
                ProcessName.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.IN_PROGRESS,
                UUID.randomUUID(),
                null
            ),
            generateProcessProgressDetails(
                processId,
                ProcessName.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.INITIALIZED,
                UUID.randomUUID(),
                null
            ),
            generateProcessProgressDetails(
                processId,
                ProcessName.CREATE_CUSTOMER_ACCOUNT,
                ProgressType.FINISHED_WITH_ERROR,
                UUID.randomUUID(),
                null
            )
        )
        coEvery { accountRepository.getByIds(any()) } returns emptyList()

        val accounts = customerAccountService.getAccountsForProcessId(processId)

        assertThat(accounts).isEmpty()
        coEvery { accountRepository.getByIds(emptyList()) }
    }
}