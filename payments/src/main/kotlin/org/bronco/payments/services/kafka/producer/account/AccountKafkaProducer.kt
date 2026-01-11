package org.bronco.payments.services.kafka.producer.account

import org.bronco.payments.services.account.model.AccountCreationData
import org.bronco.payments.services.processes.model.ProcessProgressDetails

interface AccountKafkaProducer {
    suspend fun dispatchCreateCustomerAccount(payload: AccountCreationData): ProcessProgressDetails
}
