package org.bronco.payments.repositories.progress

import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import java.util.*

data class ProcessProgressProperties(
    val id: UUID,
    val processType: ProcessType,
    val progressType: ProgressType,
    val entityId: UUID?,
    val parentProcessId: UUID?,
    val progressDetails: String?
) {
    companion object {
        fun ofInitial(processType: ProcessType, parentProcessId: UUID?): ProcessProgressProperties =
            ProcessProgressProperties(
                id = UUID.randomUUID(),
                processType = processType,
                progressType = ProgressType.INITIALIZED,
                entityId = null,
                parentProcessId = parentProcessId,
                progressDetails = null
            )

        fun ofInitial(processId: UUID, processType: ProcessType, parentProcessId: UUID?): ProcessProgressProperties =
            ProcessProgressProperties(
                id = processId,
                processType = processType,
                progressType = ProgressType.INITIALIZED,
                entityId = null,
                parentProcessId = parentProcessId,
                progressDetails = null
            )

        fun of(processId: UUID, processType: ProcessType, progressType: ProgressType, parentProcessId: UUID?): ProcessProgressProperties =
            ProcessProgressProperties(
                id = processId,
                processType = processType,
                progressType = progressType,
                entityId = null,
                parentProcessId = parentProcessId,
                progressDetails = null
            )

        fun of(processId: UUID, processType: ProcessType, progressType: ProgressType, parentProcessId: UUID? = null, entityId: UUID? = null, processDetails: String? = null): ProcessProgressProperties =
            ProcessProgressProperties(
                id = processId,
                processType = processType,
                progressType = progressType,
                entityId = entityId,
                parentProcessId = parentProcessId,
                progressDetails = processDetails
            )
    }

    fun toProgressKey(): ProgressKey = ProgressKey(id, processType)
}
