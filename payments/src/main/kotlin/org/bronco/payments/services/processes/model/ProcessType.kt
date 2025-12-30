package org.bronco.payments.services.processes.model

enum class ProcessType {
    CREATE_CUSTOMER,
    CREATE_CUSTOMER_ACCOUNT,
    UPDATE_CUSTOMER,
    DELETE_CUSTOMER,
    CHANGE_PASSWORD
    ;

    companion object {
        fun fromString(value: String?): ProcessType = entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Unknown process type was received: $value")
    }
}