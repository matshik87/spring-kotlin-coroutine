package org.bronco.payments.services.kafka.producer.customer

import com.fasterxml.jackson.databind.ObjectWriter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.bronco.payments.config.KafkaTestConfig
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.bronco.payments.model.Currencies
import org.bronco.payments.repositories.account.impl.PaymentsAccountRepository
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.repositories.customer.impl.PaymentsCustomerRepository
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.schema.jooq.model.tables.references.ACCOUNT
import org.bronco.payments.schema.jooq.model.tables.references.CUSTOMER
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
import org.bronco.payments.services.account.AccountService
import org.bronco.payments.services.customer.CustomerService
import org.bronco.payments.services.kafka.producer.account.AccountKafkaProducer
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.CustomerDataGenerators.generateCreateCustomerAccountCommand
import org.bronco.payments.utils.CustomerDataGenerators.generateCustomerData
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(BroncoKafkaProperties::class, KafkaTestConfig::class)
class CustomerAccountKafkaIntegrationTest {

    @Autowired
    private lateinit var kafkaProducer: AccountKafkaProducer

    @Autowired
    lateinit var customerService: CustomerService

    @Autowired
    private lateinit var progressRepository: ProgressRepository

    @Autowired
    private lateinit var createCustomerKafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Autowired
    private lateinit var kafkaProperties: BroncoKafkaProperties

    @Autowired
    private lateinit var customerRepository: PaymentsCustomerRepository

    @Autowired
    private lateinit var accountRepository: PaymentsAccountRepository

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var adminClient: AdminClient

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun setUp() {
        val topics = kafkaProperties.consumers.topics.map { it.topicName }
        adminClient.deleteTopics(topics).all()
        topics.forEach { topicName ->
            adminClient.createTopics(listOf(NewTopic(topicName, 1, 1))).all()
        }
        dslContext.deleteFrom(PROCESS_PROGRESS)
        dslContext.deleteFrom(ACCOUNT)
        dslContext.deleteFrom(CUSTOMER)
    }

    @AfterEach
    fun tearDown() {
        val topics = kafkaProperties.consumers.topics.map { it.topicName }
        adminClient.deleteTopics(topics).all()
        topics.forEach { topicName ->
            adminClient.createTopics(listOf(NewTopic(topicName, 1, 1))).all()
        }
        dslContext.deleteFrom(PROCESS_PROGRESS)
        dslContext.deleteFrom(ACCOUNT)
        dslContext.deleteFrom(CUSTOMER)
    }

    @Test
    fun scheduleNewAccountCreation_withValidRequest_butNoNameForAnAccount() = runTest {
        val parentProcessId = UUID.randomUUID()

        val newCustomerData = generateCustomerData()
        val customer = customerRepository.createUser(UUID.randomUUID(), newCustomerData)
        val customerId = customer.customerId!!
        val accountCreationRequest = generateCreateCustomerAccountCommand(customerId)

        accountService.scheduleNewAccountCreation(parentProcessId, accountCreationRequest)

        await().atMost(8, TimeUnit.SECONDS).until {
            runBlocking {
                progressRepository.findProcessDetailsForProcessTypesByIds(
                    processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT),
                    parentProcessId = parentProcessId
                ).all { it.progress == ProgressType.FINISHED.name }
                        && accountRepository.getAllCustomerAccounts(customerId).isNotEmpty()
            }
        }

        val result = progressRepository.findProcessDetailsForProcessTypesByIds(
            processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT),
            parentProcessId = parentProcessId
        )

        assertThat(result).isNotNull.singleElement()
            .returns(ProgressType.FINISHED.name) { it.progress }
            .returns(ProcessType.CREATE_CUSTOMER_ACCOUNT.name) { it.type }
            .returns(parentProcessId) { it.parentId }
            .returns(null) { it.details }
        assertThat(result.first().id).isNotNull()

        val account = accountRepository.getById(result.first().entityId!!)
        assertThat(account).isNotNull()
            .returns(Currencies.USD.name) { it?.currencyCode }
            .returns(AccountStatus.INACTIVE) { it?.status }
            .returns(BigDecimal.ZERO.setScale(2)) { it?.balance }
            .returns(customerId) { it?.customerId }
            .returns(null) { it?.accountName }
    }

    @Test
    fun scheduleNewAccountCreation_withValidRequest_WithAccountName() = runTest {
        val parentProcessId = UUID.randomUUID()

        val newCustomerData = generateCustomerData()
        val customer = customerRepository.createUser(UUID.randomUUID(), newCustomerData)
        val customerId = customer.customerId!!
        val accountName = "test-name"
        val accountCreationRequest = generateCreateCustomerAccountCommand(customerId, Currencies.USD, accountName)

        accountService.scheduleNewAccountCreation(parentProcessId, accountCreationRequest)

        await().atMost(8, TimeUnit.SECONDS).until {
            runBlocking {
                progressRepository.findProcessDetailsForProcessTypesByIds(
                    processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT),
                    parentProcessId = parentProcessId
                ).all { it.progress == ProgressType.FINISHED.name }
                        && accountRepository.getAllCustomerAccounts(customerId).isNotEmpty()
            }
        }

        val result = progressRepository.findProcessDetailsForProcessNamesByIds(
            processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT),
            parentProcessId = parentProcessId
        )

        assertThat(result).isNotNull.singleElement()
            .returns(ProgressType.FINISHED.name) { it.progress }
            .returns(ProcessType.CREATE_CUSTOMER_ACCOUNT.name) { it.type }
            .returns(parentProcessId) { it.parentId }
            .returns(null) { it.details }
        assertThat(result.first().id).isNotNull()

        val account = accountRepository.getById(result.first().entityId!!)
        assertThat(account).isNotNull()
            .returns(Currencies.USD.name) { it?.currencyCode }
            .returns(AccountStatus.INACTIVE) { it?.status }
            .returns(BigDecimal.ZERO.setScale(2)) { it?.balance }
            .returns(customerId) { it?.customerId }
            .returns(accountName) { it?.accountName }
    }

    @Test
    fun dispatchCreateCustomerAccount_withInvalidRequest_customerIsNotCreatedButProgressHasMoreDetails() = runTest {
        val parentProcessId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val accountCreationRequest = generateCreateCustomerAccountCommand(parentProcessId, Currencies.USD)

        accountService.scheduleNewAccountCreation(parentProcessId, accountCreationRequest)

        await().atMost(8, TimeUnit.SECONDS).until {
            runBlocking {
                progressRepository.findProcessDetailsForProcessTypesByIds(
                    processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT),
                    parentProcessId = parentProcessId
                ).all { it.progress == ProgressType.FINISHED_WITH_ERROR.name }
                        && accountRepository.getAllCustomerAccounts(customerId).isEmpty()
            }
        }

        val result = progressRepository.findProcessDetailsForProcessTypesByIds(
            processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT),
            parentProcessId = parentProcessId
        )

        assertThat(result).isNotNull.singleElement()
            .returns(ProgressType.FINISHED_WITH_ERROR.name) { it.progress }
            .returns(ProcessType.CREATE_CUSTOMER_ACCOUNT.name) { it.type }
            .returns(parentProcessId) { it.parentId }
            .returns("Customer account could not be created") { it.details }
        assertThat(result.first().id).isNotNull()
    }
}