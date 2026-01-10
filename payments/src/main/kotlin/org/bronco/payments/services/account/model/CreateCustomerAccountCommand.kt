package org.bronco.payments.services.account.model

import org.bronco.payments.services.processes.model.ProcessProgressDetails
import java.util.*

data class BatchAccountCreation(
    val requests: List<CreateCustomerAccountCommand> = emptyList()
)

data class CreateCustomerAccountCommand(
    val customerId: UUID,
    val currencyCode: String,
    val accountName: String?,
)

data class AccountCreationData(
    val customerId: UUID,
    val processId: UUID,
    val currencyCode: String,
    val parentProcessId: UUID? = null,
    val accountName: String? = null,
)

data class BatchAccountCreationResponse(
    val results: Map<UUID, List<ProcessProgressDetails>> = emptyMap()
)