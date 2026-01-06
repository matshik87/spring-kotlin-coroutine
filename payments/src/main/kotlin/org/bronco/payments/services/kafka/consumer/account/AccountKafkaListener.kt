package org.bronco.payments.services.kafka.consumer.account

import com.fasterxml.jackson.databind.ObjectReader
import kotlinx.coroutines.coroutineScope
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.services.account.AccountService
import org.bronco.payments.services.account.model.AccountCreationData
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import org.bronco.payments.utils.KafkaCommons.PARENT_PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_ID_KEY
import org.bronco.payments.utils.KafkaCommons.PROCESS_TYPE_KEY
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import java.util.*

@Component
class AccountKafkaListener(
    private val accountService: AccountService,
    private val objectReader: ObjectReader,
    private val progressRepository: ProgressRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AccountKafkaListener::class.java)
    }

    @KafkaListener(
        topics = ["\${kafka.topics.create-customer-account.topic-name}"],
        groupId = "\${kafka.topics.create-customer-account.group-id}",
        containerFactory = "customerAccountListenerContainer",
    )
    suspend fun createNewAccount(
        record: ConsumerRecord<String, String>,
        @Header(PROCESS_ID_KEY) processIdKey: String,
        @Header(PROCESS_TYPE_KEY) processTypeKey: String,
        @Header(PARENT_PROCESS_ID_KEY) parentProcessKey: String?
    ) = coroutineScope {
        if (ProcessType.CREATE_CUSTOMER_ACCOUNT.name == processTypeKey) {
            val processId = UUID.fromString(processIdKey)
            val processDetails = retrieveProcessDetails(processId, parentProcessKey)
            handleNewAccountPayload(processDetails, record, processTypeKey, processIdKey, parentProcessKey, processId)
        } else {
            logger.warn(
                "Unsupported process type was received at ${record.topic()}: [$processTypeKey, processId: $processIdKey, parentProcessId: $parentProcessKey]"
            )
        }
    }

    private suspend fun handleNewAccountPayload(
        processDetails: ProcessProgressDetails?,
        record: ConsumerRecord<String, String>,
        processTypeKey: String,
        processIdKey: String,
        parentProcessKey: String?,
        processId: UUID?
    ) = when (val progressType = ProgressType.fromString(processDetails?.progress)) {
        ProgressType.DISPATCHED -> {
            accountService.createNewAccount(objectReader.readValue(record.value(), AccountCreationData::class.java))
        }

        else -> {
            if (progressType.terminal) {
                logger.info("Process [$processTypeKey, processId: $processIdKey, parentProcessId: $parentProcessKey] was finalized.")
            } else if (progressType == ProgressType.IN_PROGRESS) {
                logger.info("Processing of request [$processTypeKey, processId: $processIdKey, parentProcessId: $parentProcessKey] is ongoing. Skipping processing...")
            } else {
                logger.warn("Unsupported progress type was received: $progressType, processId: $processId, parentProcessId: $parentProcessKey")
            }
        }
    }

    private suspend fun retrieveProcessDetails(processId: UUID, parentProcessKey: String?): ProcessProgressDetails? {
        val processTypes = listOf(ProcessType.CREATE_CUSTOMER_ACCOUNT)
        val parentProcessId = parentProcessKey?.let { value ->
            if (value.isNotBlank()) {
                UUID.fromString(value)
            } else {
                null
            }
        }
        return progressRepository.findProcessDetailsForProcessTypesByIds(
            processTypes = processTypes,
            processId = processId,
            parentProcessId = parentProcessId
        ).firstOrNull { ProcessType.CREATE_CUSTOMER_ACCOUNT.name == it.type }
    }
}