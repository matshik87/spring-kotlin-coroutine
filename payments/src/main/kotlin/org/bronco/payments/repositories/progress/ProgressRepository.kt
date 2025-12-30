package org.bronco.payments.repositories.progress

import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProcessProgressDetails
import org.bronco.payments.services.processes.model.ProgressType
import java.util.UUID

interface ProgressRepository {
    suspend fun initiateProgress(properties: ProcessProgressProperties): ProcessProgressDetails
    suspend fun updateProgress(key: ProgressKey, progress: ProgressType, id: UUID?, progressDetails: String?)
    suspend fun updateProgress(properties: ProcessProgressProperties): ProcessProgressDetails
    suspend fun retrieveProcessDetailsByName(processId: UUID, processType: ProcessType): ProcessProgressDetails?
    suspend fun retrieveProcessDetails(processId: UUID): List<ProcessProgressDetails>
    suspend fun findProcessDetailsForProcessNames(processId: UUID, processTypes: Collection<ProcessType>): List<ProcessProgressDetails>
    suspend fun findProcessDetailsForProcessNamesBuParentId(parentProcessId: UUID, processTypes: Collection<ProcessType>): List<ProcessProgressDetails>
}