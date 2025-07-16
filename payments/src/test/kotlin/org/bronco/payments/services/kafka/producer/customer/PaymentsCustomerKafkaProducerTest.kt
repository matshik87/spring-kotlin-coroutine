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
import org.bronco.payments.repositories.customer.impl.PaymentsCustomerRepository
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.schema.jooq.model.tables.references.CUSTOMER
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.CustomerDataGenerators.generateCreateCustomerRequest
import org.bronco.payments.utils.TemporalUtils.LOCAL_DATE_FORMATTER
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(BroncoKafkaProperties::class, KafkaTestConfig::class)
class PaymentsCustomerKafkaProducerTest {
    companion object {
        private val email = "email@test.com"
    }

    @Autowired
    private lateinit var kafkaProducer: PaymentsCustomerKafkaProducer

    @Autowired
    private lateinit var progressRepository: ProgressRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Autowired
    private lateinit var kafkaProperties: BroncoKafkaProperties

    @Autowired
    private lateinit var customerRepository: PaymentsCustomerRepository

    @Autowired
    private lateinit var adminClient: AdminClient

    @Autowired
    private lateinit var dslContext: DSLContext

    @AfterEach
    fun setUp() {
        val createCustomer = kafkaProperties.consumers.topics.createCustomer
        adminClient.deleteTopics(listOf(createCustomer.topicName))
        adminClient.createTopics(listOf(NewTopic(createCustomer.topicName, 1, 1)))
        dslContext.deleteFrom(PROCESS_PROGRESS)
        dslContext.deleteFrom(CUSTOMER)
    }

    @Test
    fun dispatchCreateCustomer() = runTest {
        val payload = generateCreateCustomerRequest(email)

        kafkaProducer.dispatchCreateCustomer(payload)
        customerRepository.findByLoginAndEmail(null, payload.email)
        await().atMost(1, TimeUnit.SECONDS).until {
            // Custom await using runBlocking
            runBlocking {
                val result = customerRepository.findByLoginAndEmail(null, payload.email)
                result != null
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
        assertThat(result!!.login).isNotBlank()
        assertThat(result.password).isNotBlank()
        val progress = dslContext.selectFrom(PROCESS_PROGRESS).where(
            PROCESS_PROGRESS.ENTITY_ID.eq(result.customerId)
                .and(PROCESS_PROGRESS.PROCESS_NAME.eq(ProcessName.CREATE_CUSTOMER.name))
                .and(PROCESS_PROGRESS.PROGRESS.eq(ProgressType.FINISHED.name))
        ).fetchInto(ProcessProgressDetails::class.java)
        assertThat(progress).singleElement()
    }
}