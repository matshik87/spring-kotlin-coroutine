package org.bronco.payments.repositories.progress.impl

import kotlinx.coroutines.future.await
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.schema.jooq.model.tables.records.ProcessProgressRecord
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProcessType
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.SelectQuery
import org.jooq.impl.DSL
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
open class ProcessProgressRepository(
    private val dslContext: DSLContext
) : ProgressRepository {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProcessProgressRepository::class.java)
        private fun ProcessProgressRecord.toProgressDetails(): ProcessProgressDetails = ProcessProgressDetails(
            this.processId!!, this.processType!!, this.processParentId, this.progress!!, this.entityId, this.details
        )
    }

    override suspend fun initiateProgress(
        properties: ProcessProgressProperties,
    ): ProcessProgressDetails {
        return runInTransaction { transaction ->
            val recordLambda =
                prepareRecord(properties, creationDate = LocalDateTime.now())
            val record = recordLambda(transaction)
            transaction.batchInsert(record)
                .executeAsync()
                .handleAsync { results, throwable ->
                    val key = properties.toProgressKey()
                    if (throwable != null) {
                        logger.error(
                            "Adding progress ${properties.progressType} for [${key}, id: ${properties.entityId}] has failed.",
                            throwable
                        )
                        ProcessProgressDetails.fromProcessProperties(properties, throwable.message)
                    } else {
                        logger.info("Adding progress for [${key}, id: ${properties.entityId}] with ${properties.progressType} has completed: ${results.first() > 0}.")
                        record.toProgressDetails()
                    }
                }.await()
        }
    }

    override suspend fun updateProgress(
        properties: ProcessProgressProperties,
    ): ProcessProgressDetails = runInTransaction<ProcessProgressDetails> { transaction ->
        val recordGenerator = prepareRecord(properties, modificationTime = LocalDateTime.now())
        val progressKey = properties.toProgressKey()
        val record = recordGenerator(transaction)
        transaction.batchUpdate(record).executeAsync()
            .handleAsync { results, throwable ->
                val progress = properties.progressType
                if (throwable != null) {
                    logger.error(
                        "Updating progress into $progress for [$progressKey, id: ${properties.entityId}] has failed.",
                        throwable
                    )
                    ProcessProgressDetails.fromProcessProperties(properties, throwable.message)
                } else {
                    logger.info("Updating progress for [$progressKey, id: ${properties.entityId}] with $progress has completed: ${results.first() > 0}.")
                    record.toProgressDetails()
                }
            }.await()
    }

    override suspend fun retrieveProcessDetailsByName(
        processId: UUID,
        processType: ProcessType
    ): ProcessProgressDetails? {
        return retrieveDetails(processId = processId, processType = processType).firstOrNull()
    }

    override suspend fun retrieveProcessDetails(processId: UUID): List<ProcessProgressDetails> {
        return retrieveDetails(processId)
    }

    override suspend fun findProcessDetailsForProcessTypesByIds(
        processTypes: Collection<ProcessType>,
        processId: UUID?,
        parentProcessId: UUID?
    ): List<ProcessProgressDetails> {
        require(processId != null || parentProcessId != null) { "any of main or parent process id is required" }
        return runInTransaction { transaction ->
            val query = selectQuery(transaction)

            if (processTypes.isNotEmpty()) {
                query.addConditions(
                    PROCESS_PROGRESS.PROCESS_TYPE.`in`(processTypes.map { it.name })
                )
            }
            processId?.let { query.addConditions(PROCESS_PROGRESS.PROCESS_ID.eq(it)) }
            parentProcessId?.let { query.addConditions(PROCESS_PROGRESS.PROCESS_PARENT_ID.eq(it)) }
            query.fetchInto(ProcessProgressDetails::class.java)
        }
    }

    private suspend fun retrieveDetails(
        processId: UUID,
        parentProcessId: UUID? = null,
        processType: ProcessType? = null,
    ): List<ProcessProgressDetails> {
        val processTypeList = processType?.let { listOf(it) } ?: emptyList()
        return findProcessDetailsForProcessTypesByIds(
            processTypes = processTypeList,
            processId = processId,
            parentProcessId = parentProcessId
        )
    }

    private fun selectQuery(transaction: DSLContext): SelectQuery<Record> {
        val selectQueryRoot = transaction.selectQuery()
        selectQueryRoot.addFrom(PROCESS_PROGRESS)
        return selectQueryRoot
    }

    private suspend fun <T> runInTransaction(action: suspend (DSLContext) -> T): T {
        return dslContext.transactionCoroutine { transactional ->
            action(DSL.using(transactional))
        }
    }

    private fun prepareRecord(
        properties: ProcessProgressProperties,
        creationDate: LocalDateTime? = null,
        modificationTime: LocalDateTime = LocalDateTime.now()
    ): (DSLContext) -> ProcessProgressRecord = { context ->
        context.newRecord(PROCESS_PROGRESS).apply {
            processId = properties.id
            processType = properties.processType.name
            processParentId = properties.parentProcessId
            progress = properties.progressType.toString()
            entityId = properties.entityId
            properties.progressDetails?.let { details ->
                this.details = details
            }
            if (creationDate != null) {
                this.creationDate = creationDate
            }
            modificationDate = modificationTime
        }
    }
}