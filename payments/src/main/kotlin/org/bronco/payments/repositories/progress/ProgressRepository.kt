package org.bronco.payments.repositories.progress

import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import java.util.UUID

interface ProgressRepository {
    suspend fun initiateProgress(properties: ProcessProgressProperties): ProcessProgressDetails
    suspend fun updateProgress(properties: ProcessProgressProperties): ProcessProgressDetails
    suspend fun retrieveProcessDetailsByName(processId: UUID, processType: ProcessType): ProcessProgressDetails?
    suspend fun retrieveProcessDetails(processId: UUID): List<ProcessProgressDetails>
    suspend fun findProcessDetailsForProcessNamesByIds(processTypes: Collection<ProcessType>, processId: UUID? = null, parentProcessId: UUID? = null): List<ProcessProgressDetails>
}