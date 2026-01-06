package org.bronco.payments.services.kafka.producer.account

import com.fasterxml.jackson.databind.ObjectWriter
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.bronco.payments.config.KafkaConfiguration.Companion.createCustomerAccountTopic
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.account.model.AccountCreationData
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.KafkaCommons.PARENT_PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_TYPE_KEY
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomerAccountKafkaProducer(
    @Qualifier("createCustomerKafkaTemplate")
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: BroncoKafkaProperties,
    private val objectWriter: ObjectWriter,
    private val processRepository: ProgressRepository
) : AccountKafkaProducer {
    companion object {
        private val logger = LoggerFactory.getLogger(CustomerAccountKafkaProducer::class.java)
    }
    private val topic = kafkaProperties.producers.findTopic(createCustomerAccountTopic)

    override suspend fun dispatchCreateCustomerAccount(payload: AccountCreationData): ProcessProgressDetails {
        requireNotNull(payload, { "Request payload for customer creation is required" })
        val kafkaRecord = prepareCreateCustomerAccountRecord(payload)
        val processDetails = processRepository.updateProgress(payload.toProcessProperties())
        kafkaTemplate.send(kafkaRecord)
        return processDetails
    }

    private fun prepareCreateCustomerAccountRecord(
        payload: AccountCreationData
    ): ProducerRecord<String, String> {
        val progressUuid = payload.processId.toString()
        val headers = listOf(
            RecordHeader(PROCESS_TYPE_KEY, ProcessType.CREATE_CUSTOMER_ACCOUNT.name.toByteArray()),
            RecordHeader(PROCESS_ID_KEY, progressUuid.toByteArray()),
            payload.parentProcessId?.let {parentProcessId ->
                RecordHeader(PARENT_PROCESS_ID_KEY, parentProcessId.toString().toByteArray())
            }
        )
        val recordPayload = kotlin.runCatching { objectWriter.writeValueAsString(payload) }
            .getOrNull() ?: throw IllegalStateException("Could not create customer account payload")
        return ProducerRecord(topic.topicName, null, progressUuid, recordPayload, headers)
    }

    private fun AccountCreationData.toProcessProperties(): ProcessProgressProperties = ProcessProgressProperties(
        id = this.processId,
        processType = ProcessType.CREATE_CUSTOMER_ACCOUNT,
        progressType = ProgressType.DISPATCHED,
        entityId = null,
        parentProcessId = this.parentProcessId,
        progressDetails = null
    )
}