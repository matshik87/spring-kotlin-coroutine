package org.bronco.payments.services.customer

import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import org.bronco.payments.controllers.api.RetrieveCustomerResponse
import org.bronco.payments.repositories.customer.CustomerData
import java.util.*

interface CustomerService {
    suspend fun executeIfFound(
        login: String?,
        email: String?,
        processing: suspend (CustomerData) -> Unit
    ): CustomerData?

    suspend fun executeIfFound(email: String?, processing: suspend (CustomerData) -> Unit): CustomerData?
    suspend fun retrieveCustomerById(customerId: UUID): RetrieveCustomerResponse
    suspend fun createNewCustomer(processId: UUID, parentProcessId: UUID?, customerRequest: CreateCustomerRequest): CreateCustomerResponse
    suspend fun deleteById(id: UUID): Unit
}