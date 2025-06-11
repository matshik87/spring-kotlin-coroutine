package org.bronco.payments.services.processes

import org.bronco.payments.services.processes.model.ProcessProgressDetails

interface ProcessesService {
    suspend fun updateProgress(): ProcessProgressDetails
}