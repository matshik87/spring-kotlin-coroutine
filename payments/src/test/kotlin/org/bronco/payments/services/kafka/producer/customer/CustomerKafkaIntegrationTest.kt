package org.bronco.payments.services.kafka.producer.customer

import com.fasterxml.jackson.databind.ObjectWriter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.apache.commons.lang3.RandomStringUtils
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.bronco.payments.config.KafkaConfiguration.Companion.createCustomerTopic
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
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.CustomerDataGenerators.generateCreateCustomerRequest
import org.bronco.payments.utils.TemporalUtils.LOCAL_DATE_FORMATTER
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(BroncoKafkaProperties::class, KafkaTestConfig::class)
class CustomerKafkaIntegrationTest {
    companion object {
        private val email = "email@test.com"
    }

    @Autowired
    private lateinit var kafkaProducer: PaymentsCustomerKafkaProducer

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
    private lateinit var adminClient: AdminClient

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun setUp() {
        val createCustomer = kafkaProperties.consumers.findTopic(createCustomerTopic)
        adminClient.deleteTopics(listOf(createCustomer.topicName))
        adminClient.createTopics(listOf(NewTopic(createCustomer.topicName, 1, 1)))
        dslContext.deleteFrom(PROCESS_PROGRESS)
        dslContext.deleteFrom(ACCOUNT)
        dslContext.deleteFrom(CUSTOMER)
    }

    @AfterEach
    fun tearDown() {
        dslContext.deleteFrom(PROCESS_PROGRESS)
        dslContext.deleteFrom(ACCOUNT)
        dslContext.deleteFrom(CUSTOMER)
    }

    @Test
    fun dispatchCreateCustomer_withValidRequest_customerAndAccountAreCreated() = runTest {
        val payload = generateCreateCustomerRequest(email)

        kafkaProducer.dispatchCreateCustomer(payload)
        await().atMost(5, TimeUnit.SECONDS).until {
            runBlocking {
                customerRepository.findByLoginAndEmail(null, payload.email) != null
            }
        }

        val result = customerRepository.findByLoginAndEmail(null, payload.email)

        assertThat(result).isNotNull
            .returns(payload.firstName) { it!!.firstName }
            .returns(payload.middleName) { it!!.middleName }
            .returns(payload.lastName) { it!!.lastName }
            .returns(payload.countryOfResidence) { it!!.countryOfResidence }
            .returns(LocalDate.parse(payload.dateOfBirth, LOCAL_DATE_FORMATTER)) { it!!.dateOfBirth }
            .returns(payload.nationality) { it!!.nationality }
            .returns(payload.phoneNumber) { it!!.phoneNumber }
            .returns(payload.secondaryPhoneNumber) { it!!.secondaryPhoneNumber }
        assertThat(result!!.customerId!!)
            .matches { customerId: UUID ->
                await().atMost(5, TimeUnit.SECONDS).until {
                    runBlocking {
                        accountRepository.getAllCustomerAccounts(customerId).isNotEmpty()
                    }
                }
                runBlocking {
                    assertThat(accountRepository.getCustomerAccountsByStatus(customerId, setOf(AccountStatus.INACTIVE)))
                        .singleElement()
                        .returns(Currencies.USD.name) { it.currencyCode }
                        .returns(AccountStatus.INACTIVE) { it.status }
                        .returns(BigDecimal.ZERO.setScale(2)) { it.balance }
                        .returns(customerId) { it.customerId }
                }
                true
            }
        assertThat(result.login).isNotBlank()
        assertThat(result.password).isNotBlank()
        val progress = dslContext.selectFrom(PROCESS_PROGRESS).where(
            PROCESS_PROGRESS.ENTITY_ID.eq(result.customerId)
                .and(PROCESS_PROGRESS.PROCESS_TYPE.eq(ProcessType.CREATE_CUSTOMER.name))
                .and(PROCESS_PROGRESS.PROGRESS.eq(ProgressType.FINISHED.name))
        ).fetchInto(ProcessProgressDetails::class.java)
        assertThat(progress).singleElement()
    }

    @Test
    fun dispatchCreateCustomer_withInvalidRequest_customerIsNotCreatedButProgressHasMoreDetails() = runTest {
        val email = RandomStringUtils.secure().nextAlphanumeric(101)
        val payload = generateCreateCustomerRequest(email)

        kafkaProducer.dispatchCreateCustomer(payload)

        await().atMost(1, TimeUnit.SECONDS).until {
            runBlocking {
                dslContext.fetchExists(
                dslContext.selectFrom(PROCESS_PROGRESS)
                    .where(
                        PROCESS_PROGRESS.PROGRESS.eq(ProgressType.FINISHED_WITH_ERROR.name).and(
                            PROCESS_PROGRESS.PROCESS_TYPE.eq(ProcessType.CREATE_CUSTOMER.name)
                        ).and(
                            PROCESS_PROGRESS.ENTITY_ID.isNull
                        ).and(PROCESS_PROGRESS.PROCESS_PARENT_ID.isNotNull)
                    )
            )
            }
        }

        val processProgress = dslContext.selectFrom(PROCESS_PROGRESS)
            .where(
                PROCESS_PROGRESS.PROGRESS.eq(ProgressType.FINISHED_WITH_ERROR.name).and(
                    PROCESS_PROGRESS.PROCESS_TYPE.eq(ProcessType.CREATE_CUSTOMER.name)
                ).and(
                    PROCESS_PROGRESS.PROCESS_ID.isNotNull
                ).and(PROCESS_PROGRESS.PROCESS_PARENT_ID.isNotNull)
                    .and(PROCESS_PROGRESS.ENTITY_ID.isNull)
            ).fetchInto(ProcessProgressDetails::class.java)

        assertThat(processProgress).singleElement()
    }
}