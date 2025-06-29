package org.bronco.payments.repositories.progress.impl

import kotlinx.coroutines.future.await
import org.bronco.payments.repositories.progress.ProgressKey
import org.bronco.payments.repositories.progress.ProgressRepository
import org.bronco.payments.schema.jooq.model.tables.records.ProcessProgressRecord
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
open class ProcessesProgressRepository(
    private val dslContext: DSLContext
) : ProgressRepository {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProcessesProgressRepository::class.java)
        private fun ProcessProgressRecord.toProgressDetails(): ProcessProgressDetails = ProcessProgressDetails(
            this.processId!!, this.processName!!, this.progress!!, this.entityId, this.details
        )
    }

    override suspend fun initiateProgress(
        key: ProgressKey,
        id: UUID?,
        progressDetails: String?
    ): ProcessProgressDetails {
        return runInTransaction { transaction ->
            val progress = ProgressType.INITIALIZED
            val recordLambda =
                prepareRecord(key, progress, id, details = progressDetails, creationDate = LocalDateTime.now())
            val record = recordLambda(transaction)
            transaction.batchInsert(record)
                .executeAsync()
                .handleAsync { results, throwable ->
                    if (throwable != null) {
                        logger.error("Adding progress ${progress} for [${key}, id: ${id}] has failed.", throwable)
                        ProcessProgressDetails(
                            key.id,
                            key.name.name,
                            ProgressType.FINISHED_WITH_ERROR.name,
                            id,
                            throwable.message
                        )
                    } else {
                        logger.info("Adding progress for [${key}, id: ${id}] with $progress has completed: ${results.first() > 0}.")
                        record.toProgressDetails()
                    }
                }.await()
        }
    }

    override suspend fun updateProgress(
        key: ProgressKey,
        progress: ProgressType,
        id: UUID?,
        progressDetails: String?
    ) {
        runInTransaction<Unit> { transaction ->
            val record =
                prepareRecord(key, progress, id, details = progressDetails, modificationTime = LocalDateTime.now())

            transaction.batchUpdate(record(transaction)).executeAsync()
                .handleAsync { results, throwable ->
                    if (throwable != null) {
                        logger.error(
                            "Updating progress into ${progress} for [${key}, id: ${id}] has failed.",
                            throwable
                        )
                    } else {
                        logger.info("Updating progress for [${key}, id: ${id}] with $progress has completed: ${results.first() > 0}.")
                    }
                }.await()
        }
    }

    override suspend fun retrieveProcessDetails(processId: UUID?, processName: ProcessName): ProcessProgressDetails? {
        return runInTransaction { transaction ->
            transaction.selectFrom(PROCESS_PROGRESS)
                .where(
                    PROCESS_PROGRESS.PROCESS_ID.eq(processId).and(PROCESS_PROGRESS.PROCESS_NAME.eq(processName.name))
                )
                .fetchOneInto(ProcessProgressDetails::class.java)
        }
    }

    private suspend fun <T> runInTransaction(action: suspend (DSLContext) -> T): T {
        return dslContext.transactionCoroutine { transactional ->
            action(DSL.using(transactional))
        }
    }

    private fun prepareRecord(
        key: ProgressKey,
        newProgress: ProgressType,
        id: UUID?,
        details: String? = null,
        creationDate: LocalDateTime? = null,
        modificationTime: LocalDateTime = LocalDateTime.now()
    ): (DSLContext) -> ProcessProgressRecord = { context ->
        context.newRecord(PROCESS_PROGRESS).apply {
            processId = key.id
            processName = key.name.name
            progress = newProgress.toString()
            entityId = id
            if (!details.isNullOrBlank()) {
                this.details = details
            }
            if (creationDate != null) {
                this.creationDate = creationDate
            }
            modificationDate = modificationTime
        }
    }
}