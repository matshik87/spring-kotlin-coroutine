package org.bronco.payments.services.processes.model

import jakarta.persistence.Column
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import java.util.*

data class ProcessProgressDetails(
    @Column(name = "process_id")
    val id: UUID,
    @Column(name = "process_type")
    val type: String,
    @Column(name = "process_parent_id")
    val parentId: UUID?,
    @Column(name = "progress")
    val progress: String,
    @Column(name = "entity_id")
    val entityId: UUID?,
    @Column(name = "details")
    val details: String?
) {
    companion object {
        fun fromProcessProperties(properties: ProcessProgressProperties, details: String?): ProcessProgressDetails =
            ProcessProgressDetails(
                properties.id,
                properties.processType.name,
                properties.parentProcessId,
                properties.progressType.name,
                properties.entityId,
                details
            )
    }
}