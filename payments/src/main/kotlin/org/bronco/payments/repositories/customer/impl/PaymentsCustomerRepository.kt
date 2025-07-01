package org.bronco.payments.repositories.customer.impl

import kotlinx.coroutines.future.await
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.repositories.customer.CustomerRepository
import org.bronco.payments.schema.jooq.model.tables.Customer
import org.bronco.payments.schema.jooq.model.tables.references.CUSTOMER
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.*

@Repository
open class PaymentsCustomerRepository(
    private val context: DSLContext,
) : CustomerRepository {
    private val logger = LoggerFactory.getLogger(PaymentsCustomerRepository::class.java)

    override suspend fun createUserSuspend(customerId: UUID, customerData: CustomerData): CustomerData {
        val result = context.transactionCoroutine { transactional ->
            val transaction = DSL.using(transactional)
            val record = transaction.newRecord(Customer.CUSTOMER)
                .apply {
                    id = customerId
                    firstName = customerData.firstName
                    middleName = customerData.middleName
                    lastName = customerData.lastName
                    dob = customerData.dateOfBirth
                    nationality = customerData.nationality
                    residencyCountryCode = customerData.countryOfResidence
                    login = customerData.login
                    password = customerData.password
                    email = customerData.email
                    phoneNumber = customerData.phoneNumber
                    secondaryPhoneNumber = customerData.secondaryPhoneNumber
                }
            transaction.batchInsert(record).executeAsync()
                .handleAsync { results, throwable ->
                    if (throwable != null) {
                        logger.error("Adding a new customer has failed", throwable)
                        customerData.copy(errorMessage = throwable.message)
                    } else {
                        if (results.first() != 1) {
                            customerData.copy(errorMessage = "A new customer could not be created")
                        } else {
                            record.into(CustomerData::class.java)
                                .copy(passwordChangeRequired = customerData.passwordChangeRequired)
                        }
                    }
                }.await()
        }

        return result
    }

    override suspend fun findByLoginAndEmailSuspend(login: String?, email: String?): CustomerData? {
        return context.transactionCoroutine { transactional ->
            val query = DSL.using(transactional).selectQuery(Customer.CUSTOMER)
            login?.let { query.addConditions(Customer.CUSTOMER.LOGIN.eq(login)) }
            email?.let { query.addConditions(Customer.CUSTOMER.EMAIL.eq(email)) }

            kotlin.runCatching {
                query.fetchAsync().await().firstOrNull()?.into(CustomerData::class.java)
            }
                .onFailure { exception ->
                    logger.error("Error on retrieving customer data", exception)
                }.getOrNull()
        }
    }

    override suspend fun findById(customerId: UUID): CustomerData? {
        return context.transactionCoroutine { transactional ->
            DSL.using(transactional).selectFrom(CUSTOMER)
                .where(CUSTOMER.ID.eq(customerId))
                .fetchAsync()
                .await()
                .firstOrNull()?.into(CustomerData::class.java)
        }
    }
}