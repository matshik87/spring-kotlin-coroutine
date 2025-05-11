package org.bronco.payments.services

import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import org.bronco.payments.repositories.CustomerData
import org.bronco.payments.repositories.CustomerRepository
import org.springframework.stereotype.Service

@Service
open class PaymentsCustomerService(private val repository: CustomerRepository) : CustomerService {
    private val CreateCustomerRequest.toCustomerData: CustomerData
        get() = CustomerData(
            customerId = null,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            nationality = nationality,
            login = login,
            password = password,
            email = email,
            phoneNumber = phoneNumber,
            secondaryPhoneNumber = secondaryPhoneNumber,
            errorMessage = null
        )

    private val CustomerData.toCreateCustomerResponse: CreateCustomerResponse
        get() = CreateCustomerResponse(
            customerId = customerId,
            login = login,
            email = email,
            activeAccount = isActive,
            errorDescription = errorMessage,
        )

    override fun createNewCustomer(customerRequest: CreateCustomerRequest): CreateCustomerResponse {
        return repository.createUser(customerRequest.toCustomerData).toCreateCustomerResponse
    }
}