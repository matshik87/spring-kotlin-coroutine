package org.bronco.payments.repositories.account.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.*
import org.bronco.payments.model.Currencies
import org.bronco.payments.model.ResourceCreationException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.repositories.customer.impl.PaymentsCustomerRepository
import org.bronco.payments.schema.jooq.model.tables.records.CustomerRecord
import org.bronco.payments.schema.jooq.model.tables.references.ACCOUNT
import org.bronco.payments.schema.jooq.model.tables.references.CUSTOMER
import org.bronco.payments.utils.CustomerDataGenerators.generateCustomerData
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@JooqTest(properties = ["payments.kafka.consumers.create-topics=false"])
@ComponentScan(basePackages = ["org.bronco.payments.config"])
@EnableAutoConfiguration(
    exclude = [KafkaAutoConfiguration::class]
)
@Import(PaymentsAccountRepository::class, PaymentsCustomerRepository::class)
open class PaymentsAccountRepositoryTest {
    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var repository: PaymentsAccountRepository

    @AfterEach
    open fun cleanUp(): Unit {
        dslContext.deleteFrom(ACCOUNT).execute()
        dslContext.deleteFrom(CUSTOMER).execute()
    }

    @Test
    fun createNewAccount_whenCustomerDoesntExist_returnExceptionIsThrown() = runTest {
        val customerId = UUID.randomUUID()
        val exception = ResourceCreationException(ResourceType.ACCOUNT)

        assertThatThrownBy { runBlocking { repository.createNewAccount(customerId) } }
            .usingRecursiveComparison()
            .isEqualTo(exception)
    }

    @Test
    fun createNewAccount_whenCustomerExists_accountIsCreated() = runTest {
        val customer = generateCustomer(UUID.randomUUID())
        val customerId = customer.customerId!!

        val result = repository.createNewAccount(customerId)

        assertThat(listOf(result)).usingRecursiveComparison()
            .isEqualTo(repository.getCustomerAccountsByStatus(customerId, setOf(AccountStatus.INACTIVE)))
    }

    @Test
    fun getCustomerAccountsByStatus_whenNoAccountsForCustomer_returnEmptyList() = runTest {
        val customerId = UUID.randomUUID()

        val result = repository.getCustomerAccountsByStatus(customerId, AccountStatus.entries.toSet())

        assertThat(result).isEmpty()
    }

    @Test
    fun getCustomerAccountsByStatus_whenCustomerAccountsWereFound_returnResult() = runTest {
        val customer = generateCustomer(UUID.randomUUID())
        val customerId = customer.customerId!!
        val expectation =
            listOf(
                generateAccount(customerId, AccountStatus.OPEN),
                generateAccount(customerId, AccountStatus.BLOCKED, accountBalance = BigDecimal("14.23"))
            )

        val result = repository.getCustomerAccountsByStatus(customerId, AccountStatus.entries.toSet())

        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expectation)
    }

    @Test
    fun getAllCustomerAccounts_whenAccountsForCustomerWereFound_returnResult() = runTest {
        val customer = generateCustomer(UUID.randomUUID())
        val customerId = customer.customerId!!
        val expectation =
            listOf(
                generateAccount(customerId, AccountStatus.OPEN),
                generateAccount(customerId, AccountStatus.BLOCKED, accountBalance = BigDecimal("14.23"))
            )

        val result = repository.getAllCustomerAccounts(customerId)

        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expectation)
    }

    @Test
    fun getAllCustomerAccounts_whenNoAccountForCustomerIdWasFound_returnEmptyList() = runTest {
        val customerId = UUID.randomUUID()

        val result = repository.getAllCustomerAccounts(customerId)

        assertThat(result).isEmpty()
    }

    @Test
    fun getById_whenCustomerHasAnAccount_accountIsCreated() = runTest {
        val customer = generateCustomer(UUID.randomUUID())
        val customerId = customer.customerId!!
        val account = generateAccount(customerId, AccountStatus.BLOCKED, accountBalance = BigDecimal("14.23"))

        val result = repository.getById(account.accountId)

        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(account)
    }

    @Test
    fun getById_whenNoAccountExists_nullIsReturned() = runTest {
        val accountId = UUID.randomUUID()

        val result = repository.getById(accountId)

        assertThat(result).isNull()
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

    private suspend fun generateAccount(
        customerId: UUID,
        accountStatus: AccountStatus,
        accountBalance: BigDecimal = BigDecimal("0.00")
    ): AccountData = coroutineScope {
        dslContext.transactionCoroutine { transactional ->
            val transaction = DSL.using(transactional)
            val table = ACCOUNT
            val accountRecord = transaction.newRecord(table).apply {
                id = UUID.randomUUID()
                currencyCode = Currencies.USD.name
                status = accountStatus.name
                balance = accountBalance
                creationDate = LocalDateTime.now()
                customerReference = customerId
            }
            transaction.batchInsert(accountRecord).executeAsync()
                .handleAsync { results, throwable ->
                    if (throwable != null || results.first() != 1) {
                        fail<String>("Account instance is required")
                    }
                    accountRecord.into(AccountData::class.java)
                }.await()
        }
    }
}