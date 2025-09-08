package org.bronco.payments.model

import java.util.UUID

class ResourceCreationException(resourceType: ResourceType) :
    RuntimeException("${resourceType.value.replaceFirstChar { it.uppercase() }} could not be created") {
}