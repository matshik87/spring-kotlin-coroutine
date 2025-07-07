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
        val customer = generateCustomer()

        val result = repository.findById(customer.customerId!!)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun findByLoginAndEmailSuspend_whenCustomerCouldNotBeFound_thenNullIsReturned() = runTest {
        val result = repository.findByLoginAndEmailSuspend("12345", "test@email.com")

        assertThat(result).isNull()
    }

    @Test
    fun findByLoginAndEmailSuspend_whenPartialMatchByEmail_thenEntityIsReturned() = runTest {
        val customer = generateCustomer()

        val result = repository.findByLoginAndEmailSuspend(null, customer.email)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun findByLoginAndEmailSuspend_whenPartialMatchByLogin_thenEntityIsReturned() = runTest {
        val customer = generateCustomer()

        val result = repository.findByLoginAndEmailSuspend(customer.login, null)

        assertThat(result).isEqualTo(customer)
    }

    @Test
    fun findByLoginAndEmailSuspend_whenFullMatchByLogin_thenEntityIsReturned() = runTest {
        val customer = generateCustomer()

        val result = repository.findByLoginAndEmailSuspend(customer.login, customer.email)

        assertThat(result).isEqualTo(customer)
    }

    suspend fun generateCustomer(): CustomerData = coroutineScope {
        val randomStringUtils = RandomStringUtils.secure()
        val nationality = "UK"
        dslContext.transactionCoroutine { transactional ->
            val transaction: DSLContext = DSL.using(transactional)
            val record: CustomerRecord = transaction.newRecord(CUSTOMER)
            record.id = UUID.randomUUID()
            record.firstName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            record.lastName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            record.dob = LocalDate.now().minusYears(18).minusDays(1)
            record.nationality = nationality
            record.residencyCountryCode = nationality
            record.login = randomStringUtils.nextAscii(10)
            record.password = randomStringUtils.nextAlphabetic(15)
            record.email = "${randomStringUtils.nextAscii(5, 10)}@test.com"
            record.phoneNumber = "+${randomStringUtils.nextNumeric(8)}"
            record.secondaryPhoneNumber = "+${randomStringUtils.nextNumeric(8)}"

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
}