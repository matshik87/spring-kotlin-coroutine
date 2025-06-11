package org.bronco.payments.services.customer

import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse
import java.util.*

interface CustomerService {
    fun createNewCustomer(customerRequest: CreateCustomerRequest): CreateCustomerResponse
    suspend fun createNewCustomerSuspended(processId: UUID, customerRequest: CreateCustomerRequest): CreateCustomerResponse
}