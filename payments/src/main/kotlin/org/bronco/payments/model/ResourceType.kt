package org.bronco.payments.model

enum class ResourceType(val value: String) {
    CUSTOMER("customer"),
    CUSTOMER_ACCOUNT("account for customer"),
    PROCESS("process progress"),
    ACCOUNT("customer account")
}