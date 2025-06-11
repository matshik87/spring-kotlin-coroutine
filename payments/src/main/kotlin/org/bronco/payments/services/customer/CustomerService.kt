package org.bronco.payments.services.customer

import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import org.bronco.payments.repositories.CustomerData
import java.util.*

interface CustomerService {
    fun createNewCustomer(customerRequest: CreateCustomerRequest): CreateCustomerResponse
    suspend fun createNewCustomerSuspended(processId: UUID, customerRequest: CreateCustomerRequest): CreateCustomerResponse
    suspend fun executeIfFound(login: String?, email: String?, processing: suspend (CustomerData) -> Unit): CustomerData?
    suspend fun executeIfFound(email: String?, processing: suspend (CustomerData) -> Unit): CustomerData?
}