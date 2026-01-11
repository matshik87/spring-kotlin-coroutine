package org.bronco.payments.services.customer

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
import org.bronco.payments.model.ResourceNotFoundException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.repositories.account.model.toDto
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.repositories.customer.CustomerRepository
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.account.AccountService
import org.bronco.payments.services.login.PaymentsLoginService
import org.bronco.payments.services.password.PasswordService
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.AccountDataGenerators.generateAccount
import org.bronco.payments.utils.AssertionUtils.assertProgressProperties
import org.bronco.payments.utils.CustomerDataGenerators.generateCreateCustomerRequest
import org.bronco.payments.utils.CustomerDataGenerators.generateCustomerData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.util.*

@ExtendWith(MockKExtension::class)
class PaymentsCustomerServiceTest {
    @MockK(relaxed = true)
    private lateinit var repository: CustomerRepository

    @MockK(relaxed = true)
    private lateinit var passwordService: PasswordService

    @MockK(relaxed = true)
    private lateinit var loginService: PaymentsLoginService

    @MockK(relaxed = true)
    private lateinit var progressRepository: ProgressRepository

    @MockK
    private lateinit var accountService: AccountService

    @InjectMockKs
    private lateinit var service: PaymentsCustomerService

    @Test
    fun deleteById_whenInvoked_thenIdPassedToRepository() = runTest {
        val id = UUID.randomUUID()

        service.deleteById(id)

        coVerify { repository.deleteById(id) }
    }

    @Test
    fun retrieveCustomerById_whenCustomerWasNotFoundById_thenExceptionIsThrown() = runTest {
        val id = UUID.randomUUID()
        coEvery { repository.findById(any()) } returns null
        val expectation = ResourceNotFoundException(id, ResourceType.CUSTOMER)

        assertThatThrownBy { runBlocking { service.retrieveCustomerById(id) } }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .usingRecursiveComparison()
            .isEqualTo(expectation)

        coVerify { repository.findById(id) }
    }

    @Test
    fun retrieveCustomerById_whenCustomerWasFoundById_thenInstanceIsReturned() = runTest {
        val id = UUID.randomUUID()
        val customerData = generateCustomerData(id)
        coEvery { repository.findById(any()) } returns customerData
        val account = generateAccount(customerId = id)
        coEvery { accountService.getAccountsByCustomerId(id) } returns listOf(account)

        val result = service.retrieveCustomerById(id)

        coVerify { repository.findById(id) }
        assertThat(result).isNotNull()
            .returns(id) { it.customerId }
            .returns(customerData.firstName) { it.firstName }
            .returns(customerData.middleName) { it.middleName }
            .returns(customerData.lastName) { it.lastName }
            .returns(customerData.dateOfBirth) { it.dateOfBirth }
            .returns(customerData.nationality) { it.nationality }
            .returns(customerData.countryOfResidence) { it.countryOfResidence }
            .returns(customerData.login) { it.login }
            .returns(customerData.email) { it.email }
            .returns(customerData.phoneNumber) { it.phoneNumber }
            .returns(customerData.secondaryPhoneNumber) { it.secondaryPhoneNumber }
            .returns(false) { it.passwordChangeRequired }
            .returns(listOf(account.toDto())) { it.accounts }
    }

    @Test
    fun createNewCustomer_whenCustomerWasCreatedWithoutIssues_thenProcessIsSetAsFinishedAndCreateResponseIsReturned() =
        runTest {
            val parentProcessId = UUID.randomUUID()
            val customerId = UUID.randomUUID()
            val customerRequest = generateCreateCustomerRequest()
            val customerData = generateCustomerData(null)
            val accountId = UUID.randomUUID()
            val accountData = AccountData(
                accountId,
                Currencies.USD.name,
                BigDecimal("0.00"),
                "testAccount",
                AccountStatus.OPEN,
                UUID.randomUUID()
            )
            coEvery { repository.createUser(any(UUID::class), any(CustomerData::class)) } coAnswers {
                customerData.copy(customerId = customerId)
            }
            coEvery { loginService.generateLogin { any() } } returns customerData.login
            coEvery { passwordService.encode(any()) } returns customerData.password!!
            coEvery { accountService.createAccountForABrandNewCustomer(any(), parentProcessId) } returns listOf(accountData)

            val result = service.createNewCustomer(parentProcessId, parentProcessId, customerRequest)

            coVerify { progressRepository.updateProgress(assertProgressProperties(
                ProcessType.CREATE_CUSTOMER,
                ProgressType.FINISHED,
                parentProcessId,
                entityId = customerId
            )) }
            assertThat(result).isNotNull()
                .returns(customerData.login) { it.login }
                .returns(customerData.email) { it.email }
                .returns(true) { it.activeAccount }
                .returns(true) { it.requiresPasswordChange }
                .returns(listOf(accountData.toDto())) { it.accounts }
                .returns(null) { it.errorDescription }
            assertThat(result.customerId).isNotNull()
        }

    @Test
    fun createNewCustomer_whenCustomerWasCreatedWithoutIssuesButFailedToCreateAccount_thenProcessIsSetAsFinishedAndCreateResponseIsReturned() =
        runTest {
            val parentProcessId = UUID.randomUUID()
            val customerId = UUID.randomUUID()
            val customerRequest = generateCreateCustomerRequest()
            val customerData = generateCustomerData(customerId)
            coEvery { repository.createUser(any(UUID::class), any(CustomerData::class)) } coAnswers {
                customerData
            }
            coEvery { loginService.generateLogin { any() } } returns customerData.login
            coEvery { passwordService.encode(any()) } returns customerData.password!!
            coEvery {
                accountService.createAccountForABrandNewCustomer(
                    customerId,
                    parentProcessId
                )
            } throws RuntimeException("sth failed")

            val result = service.createNewCustomer(parentProcessId, parentProcessId, customerRequest)

            coVerify { progressRepository.updateProgress(assertProgressProperties(
                ProcessType.CREATE_CUSTOMER,
                ProgressType.FINISHED,
                parentProcessId,
                entityId = customerId
            )) }
            coVerify { accountService.createAccountForABrandNewCustomer(result.customerId!!, parentProcessId) }
            assertThat(result).isNotNull()
                .returns(customerData.login) { it.login }
                .returns(customerData.email) { it.email }
                .returns(true) { it.activeAccount }
                .returns(true) { it.requiresPasswordChange }
                .returns(listOf()) { it.accounts }
                .returns(null) { it.errorDescription }
            assertThat(result.customerId).isNotNull()
        }

    @Test
    fun createNewCustomer_whenCustomerWasNotCreated_thenProcessIsSetAsFinishedWithErrors() =
        runTest {
            val parentProcessId = UUID.randomUUID()
            val customerRequest = generateCreateCustomerRequest()
            val customerData = generateCustomerData(errorMessage = "sth went wrong")
            coEvery { repository.createUser(any(UUID::class), any(CustomerData::class)) } coAnswers {
                customerData
            }
            coEvery { loginService.generateLogin { any() } } returns customerData.login
            coEvery { passwordService.encode(any()) } returns customerData.password!!

            val result = service.createNewCustomer(parentProcessId, parentProcessId, customerRequest)

            coVerify { progressRepository.updateProgress(assertProgressProperties(
                ProcessType.CREATE_CUSTOMER,
                ProgressType.FINISHED_WITH_ERROR,
                parentProcessId,
                errorMessage = customerData.errorMessage
            )) }
            assertThat(result).isNotNull()
                .returns(customerData.login) { it.login }
                .returns(customerData.email) { it.email }
                .returns(true) { it.activeAccount }
                .returns(true) { it.requiresPasswordChange }
                .returns(customerData.errorMessage) { it.errorDescription }
            assertThat(result.customerId).isNull()
        }
}