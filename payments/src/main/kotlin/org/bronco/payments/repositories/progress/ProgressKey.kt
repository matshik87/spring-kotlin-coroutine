package org.bronco.payments.repositories.progress

import org.bronco.payments.services.processes.model.ProcessName
import java.util.UUID

data class ProgressKey(
    val id: UUID,
    val name: ProcessName,
)
