package org.bronco.payments.services.customer

import org.bronco.payments.DateUtils.convertStringToLocalDate
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import org.bronco.payments.repositories.CustomerData
import org.bronco.payments.repositories.CustomerRepository
import org.bronco.payments.services.login.PaymentsLoginService
import org.bronco.payments.services.password.PasswordService
import org.springframework.stereotype.Service

@Service
open class PaymentsCustomerService(
    private val repository: CustomerRepository,
    private val passwordService: PasswordService,
    private val loginService: PaymentsLoginService,
) : CustomerService {

    private val CreateCustomerRequest.toNewCustomerData: CustomerData
        get() {
            return CustomerData(
                customerId = null,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                dateOfBirth = convertStringToLocalDate(dateOfBirth),
                nationality = nationality,
                login = loginService.generateLogin { firstName + middleName + lastName + email },
                password = passwordService.encode(password),
                email = email,
                phoneNumber = phoneNumber,
                secondaryPhoneNumber = secondaryPhoneNumber,
                errorMessage = null,
                passwordChangeRequired = if (password == null) true else false
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

    override fun createNewCustomer(customerRequest: CreateCustomerRequest): CreateCustomerResponse {
        return repository.createUser(customerRequest.toNewCustomerData).toCreateCustomerResponse
    }
}