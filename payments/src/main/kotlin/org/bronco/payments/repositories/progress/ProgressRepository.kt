package org.bronco.payments.repositories.progress

import org.bronco.payments.services.processes.model.ProgressType
import java.util.UUID

interface ProgressRepository {
    suspend fun initiateProgress(key: ProgressKey, id: UUID?, progressDetails: String? = null)
    suspend fun updateProgress(key: ProgressKey, progress: ProgressType, id: UUID?, progressDetails: String?)
}