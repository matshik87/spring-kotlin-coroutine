package org.bronco.payments.services.kafka.producer.customer

import com.fasterxml.jackson.databind.ObjectWriter
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.bronco.payments.config.KafkaConfiguration.Companion.createCustomerTopic
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.utils.KafkaCommons.PARENT_PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_TYPE_KEY
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class PaymentsCustomerKafkaProducer(
    @Qualifier("createCustomerKafkaTemplate")
    private val kafkaTemplate: KafkaTemplate<String, String>,
    kafkaProperties: BroncoKafkaProperties,
    private val objectWriter: ObjectWriter,
    private val processRepository: ProgressRepository
) : CustomerKafkaProducer {
    companion object {
        private val logger = LoggerFactory.getLogger(PaymentsCustomerKafkaProducer::class.java)
    }
    private val topic = kafkaProperties.producers.findTopic(createCustomerTopic)

    override suspend fun dispatchCreateCustomer(payload: CreateCustomerRequest): ProcessProgressDetails {
        requireNotNull(payload, { "Request payload for customer creation is required" })
        val properties = ProcessProgressProperties.ofInitial(UUID.randomUUID(), ProcessType.CREATE_CUSTOMER, UUID.randomUUID())
        val kafkaRecord = prepareCreateCustomerRecord(properties, payload)
        val processDetails = processRepository.initiateProgress(properties)
        kafkaTemplate.send(kafkaRecord)
        return processDetails
    }

    private fun prepareCreateCustomerRecord(
        properties: ProcessProgressProperties,
        payload: CreateCustomerRequest
    ): ProducerRecord<String, String> {
        val progressUuid = properties.id.toString()
        val parentProcessId = properties.parentProcessId ?: UUID.randomUUID()
        val headers = listOf(
            RecordHeader(PROCESS_TYPE_KEY, ProcessType.CREATE_CUSTOMER.name.toByteArray()),
            RecordHeader(PROCESS_ID_KEY, progressUuid.toByteArray()),
            RecordHeader(PARENT_PROCESS_ID_KEY, parentProcessId.toString().toByteArray())
        )
        val recordPayload = kotlin.runCatching { objectWriter.writeValueAsString(payload) }
            .getOrNull() ?: throw IllegalStateException("Could not create customer record")
        return ProducerRecord(topic.topicName, null, progressUuid, recordPayload, headers)
    }
}