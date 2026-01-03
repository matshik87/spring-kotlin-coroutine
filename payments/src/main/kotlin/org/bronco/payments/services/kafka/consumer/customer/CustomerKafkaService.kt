package org.bronco.payments.services.kafka.consumer.customer

import com.fasterxml.jackson.databind.ObjectReader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.customer.CustomerService
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.KafkaCommons.PARENT_PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_TYPE_KEY
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
        topics = ["\${kafka.topics.create-customer.topic-name}"],
        groupId = "\${kafka.topics.create-customer.group-id}",
        containerFactory = "customerListenerContainer",
    )
    suspend fun processCreateCustomerMessage(
        record: ConsumerRecord<String, String>,
        @Header(PROCESS_ID_KEY) processIdKey: String,
        @Header(PROCESS_TYPE_KEY) processTypeKey: String,
        @Header(PARENT_PROCESS_ID_KEY) parentProcessKey: String?
    ) = coroutineScope {
        val key = record.key()
        require(processIdKey == key) { "Process entity key must much between key and header" }
        val processId = UUID.fromString(key)
        val parentProcessId = parentProcessKey?.let { UUID.fromString(it) } ?: UUID.randomUUID()
        val processType = ProcessType.CREATE_CUSTOMER
        require(processTypeKey == processType.name) { "Process type is required to be of customer creation type" }

        val processDetails = async {
            progressRepository.retrieveProcessDetailsByName(processId, processType)
        }.await()

        when {
            processDetails == null -> throw IllegalArgumentException("Process entity [$processId] does not exist")
            processDetails.progress == ProgressType.IN_PROGRESS.name -> logger.info("$processId is already being processed in progress")
            processDetails.progress == ProgressType.INITIALIZED.name -> runCatching {
                val properties = ProcessProgressProperties.of(
                    processId,
                    processType,
                    ProgressType.IN_PROGRESS,
                    parentProcessId
                )
                progressRepository.updateProgress(properties)
                val payload = objectReader.readValue(record.value(), CreateCustomerRequest::class.java)
                customerService.executeIfFound(payload.email) { entity ->
                    progressRepository.updateProgress(properties.copy(progressType = ProgressType.ALREADY_PROCESSED, entityId = entity.customerId))
                } ?: customerService.createNewCustomer(processId, parentProcessId, payload)
            }.onFailure { throwable ->
                launch {
                    val properties = ProcessProgressProperties.of(
                        processId,
                        ProcessType.CREATE_CUSTOMER,
                        ProgressType.FINISHED_WITH_ERROR,
                        parentProcessId = parentProcessId,
                        processDetails = throwable.message
                    )
                    progressRepository.updateProgress(properties)
                }
            }

            processDetails.progress == ProgressType.FINISHED.name -> logger.info("Process ${processDetails.id} was finalized")
            processDetails.progress == ProgressType.FINISHED_WITH_ERROR.name -> logger.info("Process ${processDetails.id} has already finished with error: ${processDetails.details}")
            else -> logger.warn("Unknown process type ${processDetails.type} was assigned to ${processDetails.id}")
        }
    }
}