package org.bronco.payments.utils

import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import java.util.*

object ProcessProgressGenerators {
    fun generateProcessProgressDetails(
        processId: UUID,
        parentProcessId: UUID?,
        processType: ProcessType,
        progress: ProgressType,
        entityUuid: UUID?,
        processDetails: String?
    ): ProcessProgressDetails {
        return ProcessProgressDetails(
            id = processId,
            type = processType.name,
            progress = progress.name,
            entityId = entityUuid,
            details = processDetails,
            parentId = parentProcessId
        )
    }
}