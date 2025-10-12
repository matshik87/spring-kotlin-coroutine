package org.bronco.payments.services.customer

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import org.bronco.payments.controllers.api.RetrieveCustomerResponse
import org.bronco.payments.model.ResourceNotFoundException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.toDto
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.repositories.customer.CustomerRepository
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.account.AccountService
import org.bronco.payments.services.login.PaymentsLoginService
import org.bronco.payments.services.password.PasswordService
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.DateUtils.convertStringToLocalDate
import org.springframework.stereotype.Service
import java.util.*

@Service
open class PaymentsCustomerService(
    private val customerRepository: CustomerRepository,
    private val passwordService: PasswordService,
    private val loginService: PaymentsLoginService,
    private val progressRepository: ProgressRepository,
    private val accountService: AccountService,
) : CustomerService {
    companion object {
        private fun isPasswordChangeRequired(password: String?) = password.isNullOrBlank()
    }

    private val CreateCustomerRequest.toNewCustomerData: CustomerData
        get() {
            return CustomerData(
                customerId = null,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                dateOfBirth = convertStringToLocalDate(dateOfBirth),
                nationality = nationality,
                countryOfResidence = if (countryOfResidence.isNullOrBlank()) nationality else countryOfResidence,
                login = loginService.generateLogin { firstName + middleName + lastName + email },
                password = passwordService.encode(password),
                email = email,
                phoneNumber = phoneNumber,
                secondaryPhoneNumber = secondaryPhoneNumber,
                errorMessage = null,
                passwordChangeRequired = isPasswordChangeRequired(password)
            )
        }

    private val CustomerData.toCreateCustomerResponse: CreateCustomerResponse
        get() = CreateCustomerResponse(
            customerId = customerId,
            login = login,
            email = email,
            activeAccount = true,
            requiresPasswordChange = passwordChangeRequired ?: true,
            errorDescription = errorMessage,
        )

    private val CustomerData.toRetrievalResponse: RetrieveCustomerResponse
        get() = RetrieveCustomerResponse(
            customerId = customerId,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            nationality = nationality,
            countryOfResidence = countryOfResidence,
            login = login,
            email = email,
            phoneNumber = phoneNumber,
            secondaryPhoneNumber = secondaryPhoneNumber,
            passwordChangeRequired = isPasswordChangeRequired(password)
        )

    override suspend fun createNewCustomer(
        processId: UUID,
        customerRequest: CreateCustomerRequest
    ): CreateCustomerResponse =
        withContext(CoroutineName("createNewCustomerOrRetrieveExisting")) {

            val customerId = UUID.randomUUID()

            val userCreation = async {
                customerRepository.createUser(customerId, customerRequest.toNewCustomerData)
            }
            val customerData = userCreation.await()
            launch {
                val key = ProgressKey(processId, ProcessName.CREATE_CUSTOMER)
                if (customerData.errorMessage.isNullOrBlank()) {
                    progressRepository.updateProgress(key, ProgressType.FINISHED, customerId, customerData.errorMessage)
                } else {
                    progressRepository.updateProgress(
                        key,
                        ProgressType.FINISHED_WITH_ERROR,
                        customerId,
                        customerData.errorMessage
                    )
                }
            }
            if (customerData.errorMessage.isNullOrBlank()) {
                async {
                    runCatching { accountService.createNewAccountOrRetrieveAllExistingAccounts(customerId, processId) }
                        .getOrNull()?.let { accounts ->
                            customerData.toCreateCustomerResponse(accounts)
                        }
                }.await() ?: customerData.toCreateCustomerResponse
            } else {
                customerData.toCreateCustomerResponse
            }
        }

    override suspend fun executeIfFound(
        login: String?,
        email: String?,
        processing: suspend (CustomerData) -> Unit
    ): CustomerData? {
        val findById = customerRepository.findByLoginAndEmail(login, email)
        return findById?.let { entity ->
            processing(entity)
            entity
        }
    }

    override suspend fun executeIfFound(
        email: String?,
        processing: suspend (CustomerData) -> Unit
    ): CustomerData? = executeIfFound(null, email, processing)

    override suspend fun retrieveCustomerById(customerId: UUID): RetrieveCustomerResponse {
        return customerRepository.findById(customerId)?.let { customer ->
            customer.toRetrievalResponse(accountService.getAccountsByCustomerId(customer.customerId!!))
        } ?: throw ResourceNotFoundException(customerId, ResourceType.CUSTOMER)
    }

    override suspend fun deleteById(id: UUID): Unit {
        customerRepository.deleteById(id)
    }

    private fun CustomerData.toCreateCustomerResponse(accounts: List<AccountData> = emptyList()): CreateCustomerResponse =
        this.toCreateCustomerResponse.copy(accounts = accounts.map { account -> account.toDto() })

    private fun CustomerData.toRetrievalResponse(accounts: List<AccountData> = emptyList()): RetrieveCustomerResponse =
        this.toRetrievalResponse.copy(accounts = accounts.map { account -> account.toDto() })
}