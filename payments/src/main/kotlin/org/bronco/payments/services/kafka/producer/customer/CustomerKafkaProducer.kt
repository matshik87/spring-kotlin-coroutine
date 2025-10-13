package org.bronco.payments.services.kafka.producer.customer

import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.services.processes.model.ProcessProgressDetails

interface CustomerKafkaProducer {
    suspend fun dispatchCreateCustomer(payload: CreateCustomerRequest): ProcessProgressDetails?
}