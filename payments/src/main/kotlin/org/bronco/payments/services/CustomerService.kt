package org.bronco.payments.services

import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.CreateCustomerResponse

interface CustomerService {
    fun createNewCustomer(customerRequest: CreateCustomerRequest): CreateCustomerResponse
}