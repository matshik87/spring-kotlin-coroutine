package org.bronco.payments.model

import java.util.UUID

class ResourceNotFoundException(id: UUID, resourceType: ResourceType) :
    RuntimeException("${resourceType.value.replaceFirstChar { it.uppercase() }} with id $id was not found") {
}