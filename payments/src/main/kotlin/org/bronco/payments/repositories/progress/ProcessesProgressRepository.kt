package org.bronco.payments.repositories.progress

import org.bronco.payments.schema.jooq.model.tables.records.ProcessProgressRecord
import org.bronco.payments.schema.jooq.model.tables.references.PROCESS_PROGRESS
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
    }

    override suspend fun initiateProgress(
        key: ProgressKey,
        id: UUID?,
        progressDetails: String?
    ) {
        runInTransaction { transaction ->
            val progress = ProgressType.IN_PROGRESS
            val record =
                prepareRecord(key, progress, id, details = progressDetails, creationDate = LocalDateTime.now())
            transaction.batchInsert(record(transaction))
                .executeAsync()
                .handleAsync { results, throwable ->
                    if (throwable != null) {
                        logger.error("Adding progress ${progress} for [${key}, id: ${id}] has failed.", throwable)
                    } else {
                        logger.info("Adding progress for [${key}, id: ${id}] with $progress has completed: ${results.first() > 0}.")
                    }
                }
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
                }
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