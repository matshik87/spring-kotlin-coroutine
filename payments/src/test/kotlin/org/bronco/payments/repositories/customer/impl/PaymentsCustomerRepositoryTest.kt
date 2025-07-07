package org.bronco.payments.repositories.customer.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.bronco.payments.initializers.PostgreSqlInitializer
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.schema.jooq.model.tables.records.CustomerRecord
import org.bronco.payments.schema.jooq.model.tables.references.CUSTOMER
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.*

@JooqTest(properties = ["payments.kafka.consumers.create-topics=false"])
@ComponentScan(basePackages = ["org.bronco.payments.config"])
@EnableAutoConfiguration(
    exclude = [KafkaAutoConfiguration::class]
)
@Import(PaymentsCustomerRepository::class)
@ContextConfiguration(initializers = [PostgreSqlInitializer::class])
open class PaymentsCustomerRepositoryTest {
    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var repository: PaymentsCustomerRepository

    @BeforeEach
    fun cleanUp() {
        dslContext.deleteFrom(CUSTOMER).execute()
    }

    @Test
    fun findById_whenCustomerDoesntExist_thenNullIsReturned() = runTest {
        val customerId = UUID.randomUUID()

        val result = repository.findById(customerId)

        assertThat(result).isNull()
    }

    @Test
    fun findById_whenCustomerExist_thenEntityIsReturned() = runTest {
        val customer = generateCustomer(UUID.randomUUID())

        val result = repository.findById(customer.customerId!!)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun findByLoginAndEmail_whenCustomerCouldNotBeFound_thenNullIsReturned() = runTest {
        val result = repository.findByLoginAndEmail("12345", "test@email.com")

        assertThat(result).isNull()
    }

    @Test
    fun findByLoginAndEmail_whenPartialMatchByEmail_thenEntityIsReturned() = runTest {
        val customer = generateCustomer(UUID.randomUUID())

        val result = repository.findByLoginAndEmail(null, customer.email)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun findByLoginAndEmail_whenPartialMatchByLogin_thenEntityIsReturned() = runTest {
        val customer = generateCustomer(UUID.randomUUID())

        val result = repository.findByLoginAndEmail(customer.login, null)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun findByLoginAndEmailwhenFullMatchByLogin_thenEntityIsReturned() = runTest {
        val customer = generateCustomer(UUID.randomUUID())

        val result = repository.findByLoginAndEmail(customer.login, customer.email)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun createUser_whenCustomerWasCreated_thenEntityIsReturned() = runTest {
        val customerId = UUID.randomUUID()
        val customer = generateCustomerData()

        val result = repository.createUser(customerId, customer)

        assertThat(result).isEqualTo(customer.copy(customerId = customerId))
    }

    @Test
    fun createUser_whenCustomerCouldNotBePersisted_thenResponseIncludesErrorMessage() = runTest {
        val customerId = UUID.randomUUID()
        val customer = generateCustomerData().copy(firstName = RandomStringUtils.secure().nextAscii(41))

        val result = repository.createUser(customerId, customer)

        assertThat(result.errorMessage).isNotBlank()
        assertThat(repository.findById(customerId)).isNull()
    }

    private suspend fun generateCustomer(customerId: UUID? = null): CustomerData = coroutineScope {
        val randomizedCustomerData = generateCustomerData(customerId)
        dslContext.transactionCoroutine { transactional ->
            val transaction: DSLContext = DSL.using(transactional)
            val record: CustomerRecord = transaction.newRecord(CUSTOMER)
            record.id = randomizedCustomerData.customerId
            record.firstName = randomizedCustomerData.firstName
            record.middleName = randomizedCustomerData.middleName
            record.lastName = randomizedCustomerData.lastName
            record.dob = randomizedCustomerData.dateOfBirth
            record.nationality = randomizedCustomerData.nationality
            record.residencyCountryCode = randomizedCustomerData.countryOfResidence
            record.login = randomizedCustomerData.login
            record.password = randomizedCustomerData.password
            record.email = randomizedCustomerData.email
            record.phoneNumber = randomizedCustomerData.phoneNumber
            record.secondaryPhoneNumber = randomizedCustomerData.secondaryPhoneNumber

            transaction.batchInsert(record).executeAsync()
                .handleAsync { result, throwable ->
                    if (throwable != null) {
                        fail<String>("Customer instance could not be created")
                    }
                    if (result.first() == 0) {
                        fail<String>("No record was created")
                    }
                    record.into(CustomerData::class.java)
                }
                .await()
            record.into(CustomerData::class.java)
        }
    }

    private suspend fun generateCustomerData(customerId: UUID? = null): CustomerData = coroutineScope {
        val randomStringUtils = RandomStringUtils.secure()
        val nationality = "UK"
        CustomerData(
            customerId = customerId,
            firstName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            middleName = null,
            lastName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            dateOfBirth = LocalDate.now().minusYears(18).minusDays(1),
            nationality = nationality,
            countryOfResidence = nationality,
            login = randomStringUtils.nextAscii(10),
            password = randomStringUtils.nextAlphabetic(15),
            email = "${randomStringUtils.nextAscii(5, 10)}@test.com",
            phoneNumber = "+${randomStringUtils.nextNumeric(8)}",
            secondaryPhoneNumber = "+${randomStringUtils.nextNumeric(8)}"
        )

    }
}