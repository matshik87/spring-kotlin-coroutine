package org.bronco.payments.repositories.customer

import java.util.*

interface CustomerRepository {
    suspend fun createUserSuspend(customerId: UUID, customerData: CustomerData): CustomerData
    suspend fun findByLoginAndEmailSuspend(login: String?, email: String?): CustomerData?
    suspend fun findById(customerId: UUID): CustomerData?
}