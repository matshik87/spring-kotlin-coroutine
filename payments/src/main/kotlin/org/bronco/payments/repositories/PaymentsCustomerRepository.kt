package org.bronco.payments.repositories

import org.bronco.payments.schema.jooq.model.tables.Customer
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.*

@Repository
open class PaymentsCustomerRepository(
    private val context: DSLContext
) : CustomerRepository {
    private val logger = LoggerFactory.getLogger(PaymentsCustomerRepository::class.java)

    override fun createUser(customerData: CustomerData): CustomerData {
        return findByLoginAndEmail(customerData.login, customerData.email)
            ?: context.transactionResult { transaction ->
                val record = DSL.using(transaction).newRecord(Customer.CUSTOMER)
                    .apply {
                        id = UUID.randomUUID()
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
                kotlin.runCatching { record.store() }
                    .onFailure { exception ->
                        logger.error("Adding a new customer has failed", exception)
                    }.getOrNull()?.let { affectedRows ->
                        if (affectedRows != 1) {
                            customerData.copy(errorMessage = "A new customer could not be created")
                        } else {
                            record.into(CustomerData::class.java).copy(passwordChangeRequired = customerData.passwordChangeRequired)
                        }
                    } ?: customerData.copy(errorMessage = "A new customer could not be created")

            }
    }

    override fun findByLoginAndEmail(login: String?, email: String?): CustomerData? {
        return context.transactionResult { transaction ->
            val query = DSL.using(transaction).selectFrom(Customer.CUSTOMER)
                .query
            login?.let { query.addConditions(Customer.CUSTOMER.LOGIN.eq(login)) }
            email?.let { query.addConditions(Customer.CUSTOMER.EMAIL.eq(email)) }

            kotlin.runCatching { query.fetchOneInto(CustomerData::class.java) }
                .onFailure { exception ->
                    logger.error("Error on retrieving customer data", exception)
                }.getOrNull()
        }
    }
}