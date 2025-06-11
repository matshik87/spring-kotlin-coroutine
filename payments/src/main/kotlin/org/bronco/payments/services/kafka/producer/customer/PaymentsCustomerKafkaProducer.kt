package org.bronco.payments.services.kafka.producer.customer

import com.fasterxml.jackson.databind.ObjectWriter
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class PaymentsCustomerKafkaProducer(
    @Qualifier("defaultKafkaTemplate")
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaProperties: BroncoKafkaProperties,
    private val objectWriter: ObjectWriter,
    private val processRepository: ProgressRepository
) : CustomerKafkaProducer {
    companion object {
        const val PROCESS_TYPE_KEY = "ProcessType"
        const val PROCESS_ID_KEY: String = "ProcessId"
        private val logger = LoggerFactory.getLogger(PaymentsCustomerKafkaProducer::class.java)
    }

    override suspend fun dispatchCreateCustomer(payload: CreateCustomerRequest): ProcessProgressDetails {
        requireNotNull(payload, { "Request payload for customer creation is required" })
        val processId = UUID.randomUUID()
        val kafkaRecord = prepareCreateCustomerRecord(processId, payload)
        val processDetails = processRepository.initiateProgress(ProgressKey(processId, ProcessName.CREATE_CUSTOMER), id = null)
        kafkaTemplate.send(kafkaRecord)
        return processDetails
    }

    private fun prepareCreateCustomerRecord(
        progressId: UUID,
        payload: CreateCustomerRequest
    ): ProducerRecord<String, String> {
        val progressUuid = progressId.toString()
        val headers = listOf(
            RecordHeader(PROCESS_TYPE_KEY, ProcessName.CREATE_CUSTOMER.name.toByteArray()),
            RecordHeader(PROCESS_ID_KEY, progressUuid.toByteArray())
        )
        val recordPayload = kotlin.runCatching { objectWriter.writeValueAsString(payload) }
            .getOrElse { TODO("Could not write payload to producer - replace it with an actual exception etc") }
        return ProducerRecord(kafkaProperties.producers.createCustomer, null, progressUuid, recordPayload, headers)
    }
}