package org.bronco.payments.repositories

import java.util.*

interface CustomerRepository {
    fun createUser(customerData: CustomerData): CustomerData
    suspend fun createUserSuspend(customerId: UUID, customerData: CustomerData): CustomerData
    fun findByLoginAndEmail(login: String?, email: String?): CustomerData?
    suspend fun findByLoginAndEmailSuspend(login: String?, email: String?): CustomerData?
}