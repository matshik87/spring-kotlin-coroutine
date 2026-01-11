package org.bronco.payments.repositories.progress

import org.bronco.payments.services.processes.model.ProcessType
import java.util.UUID

data class ProgressKey(
    val id: UUID,
    val name: ProcessType,
)
