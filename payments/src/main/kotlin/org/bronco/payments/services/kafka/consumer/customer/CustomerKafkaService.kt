package org.bronco.payments.services.kafka.consumer.customer

import com.fasterxml.jackson.databind.ObjectReader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.customer.CustomerService
import org.bronco.payments.services.kafka.producer.customer.PaymentsCustomerKafkaProducer.Companion.PROCESS_ID_KEY
import org.bronco.payments.services.kafka.producer.customer.PaymentsCustomerKafkaProducer.Companion.PROCESS_TYPE_KEY
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProgressType
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomerKafkaService(
    private val customerService: CustomerService,
    private val objectReader: ObjectReader,
    private val progressRepository: ProgressRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CustomerKafkaService::class.java)
    }

    @KafkaListener(
        topics = ["\${payments.kafka.consumers.topics.create-customer.topic-name}"],
        groupId = "\${payments.kafka.consumers.topics.create-customer.group-id}",
        containerFactory = "customerListenerContainer",
    )
    suspend fun processCreateCustomerMessage(
        record: ConsumerRecord<String, String>,
        @Header(PROCESS_ID_KEY) processIdKey: String,
        @Header(PROCESS_TYPE_KEY) processTypeKey: String
    ) = coroutineScope {
        val key = record.key()
        require(processIdKey == key) { "Process entity key must much between key and header" }
        val processId = UUID.fromString(key)
        val processName = ProcessName.CREATE_CUSTOMER
        require(processTypeKey == processName.name) { "Process type is required to be of customer creation type" }

        val processDetails = async {
            progressRepository.retrieveProcessDetails(processId, processName)
        }.await()

        when {
            processDetails == null -> throw IllegalArgumentException("Process entity [$processId] does not exist")
            processDetails.progress == ProgressType.IN_PROGRESS.name -> logger.info("$processId is already being processed in progress")
            processDetails.progress == ProgressType.INITIALIZED.name -> runCatching {
                progressRepository.updateProgress(
                    ProgressKey(processId, ProcessName.CREATE_CUSTOMER),
                    ProgressType.IN_PROGRESS,
                    null, null
                )
                val payload = objectReader.readValue(record.value(), CreateCustomerRequest::class.java)
                customerService.executeIfFound(payload.email) { entity ->
                    val key = ProgressKey(processId, ProcessName.CREATE_CUSTOMER)
                    progressRepository.updateProgress(key, ProgressType.ALREADY_PROCESSED, entity.customerId, null)
                } ?: customerService.createNewCustomer(processId, payload)
            }.onFailure { throwable ->
                launch {
                    progressRepository.updateProgress(
                        ProgressKey(processId, ProcessName.CREATE_CUSTOMER),
                        ProgressType.FINISHED_WITH_ERROR,
                        null,
                        throwable.message
                    )
                }
            }

            processDetails.progress == ProgressType.FINISHED.name -> logger.info("Process ${processDetails.id} was finalized")
            processDetails.progress == ProgressType.FINISHED_WITH_ERROR.name -> logger.info("Process ${processDetails.id} has already finished with error: ${processDetails.details}")
            else -> logger.warn("Unknown process type ${processDetails.name} was assigned to ${processDetails.id}")
        }
    }
}