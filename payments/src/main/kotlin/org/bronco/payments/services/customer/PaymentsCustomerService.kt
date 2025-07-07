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
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.repositories.customer.CustomerRepository
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.login.PaymentsLoginService
import org.bronco.payments.services.password.PasswordService
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.DateUtils.convertStringToLocalDate
import org.springframework.stereotype.Service
import java.util.*

@Service
open class PaymentsCustomerService(
    private val repository: CustomerRepository,
    private val passwordService: PasswordService,
    private val loginService: PaymentsLoginService,
    private val progressRepository: ProgressRepository,
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
            customerId = null,
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
                val result = repository.createUser(customerId, customerRequest.toNewCustomerData)
                result
            }
            val customerData = userCreation.await()
            launch {
                val progressType = if (customerData.errorMessage.isNullOrBlank()) ProgressType.FINISHED
                else ProgressType.FINISHED_WITH_ERROR
                val key = ProgressKey(processId, ProcessName.CREATE_CUSTOMER)
                progressRepository.updateProgress(key, progressType, customerId, customerData.errorMessage)
            }
            customerData.toCreateCustomerResponse
        }

    override suspend fun executeIfFound(
        login: String?,
        email: String?,
        processing: suspend (CustomerData) -> Unit
    ): CustomerData? {
        val findById = repository.findByLoginAndEmail(login, email)
        return findById?.let { entity ->
            processing(entity)
            entity
        }
    }

    override suspend fun executeIfFound(
        email: String?,
        processing: suspend (CustomerData) -> Unit
    ): CustomerData? = executeIfFound(null, email, processing)

    override suspend fun retrieveCustomerById(customerId: UUID): RetrieveCustomerResponse =
        (repository.findById(customerId)?.toRetrievalResponse)
        ?: throw ResourceNotFoundException(customerId, ResourceType.CUSTOMER)
}