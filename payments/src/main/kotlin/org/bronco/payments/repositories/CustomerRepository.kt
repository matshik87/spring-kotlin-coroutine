package org.bronco.payments.repositories

interface CustomerRepository {
    fun createUser(customerData: CustomerData): CustomerData
    fun findByLoginAndEmail(login: String?, email: String?): CustomerData?
}