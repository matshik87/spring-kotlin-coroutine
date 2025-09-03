package org.bronco.payments.repositories

import org.bronco.payments.model.ResourceType
import java.util.*

class ResourceCouldNotBeenRemoved(
    val entityId: UUID,
    val type: ResourceType,
    message: String,
    val details: String? = null
) : RuntimeException(message) {
}