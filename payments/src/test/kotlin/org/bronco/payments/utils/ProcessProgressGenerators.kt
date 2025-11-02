package org.bronco.payments.utils

import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import java.util.*

object ProcessProgressGenerators {
    fun generateProcessProgressDetails(
        processId: UUID,
        processName: ProcessName,
        progress: ProgressType,
        entityUuid: UUID?,
        processDetails: String?
    ): ProcessProgressDetails {
        return ProcessProgressDetails(
            id = processId,
            name = processName.name,
            progress = progress.name,
            entityId = entityUuid,
            details = processDetails,
        )
    }
}