package org.bronco.payments.repositories.customer

import java.util.*

interface CustomerRepository {
    suspend fun createUser(customerId: UUID, customerData: CustomerData): CustomerData
    suspend fun findByLoginAndEmail(login: String?, email: String?): CustomerData?
    suspend fun findById(customerId: UUID): CustomerData?
    suspend fun deleteById(id: UUID): Unit
}