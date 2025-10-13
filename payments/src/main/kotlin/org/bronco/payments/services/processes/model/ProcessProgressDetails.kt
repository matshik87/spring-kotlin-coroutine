package org.bronco.payments.services.processes.model

import jakarta.persistence.Column
import java.util.UUID

data class ProcessProgressDetails(
    @Column(name = "process_id")
    val id: UUID,
    @Column(name = "process_name")
    val name: String,
    @Column(name = "progress")
    val progress: String,
    @Column(name = "entity_id")
    val entityId: UUID?,
    @Column(name = "details")
    val details: String?
) {
}