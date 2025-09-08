package org.bronco.payments.repositories.account.model

enum class AccountStatus(val description: String) {
    INACTIVE("inactive"),
    OPEN("open"),
    CLOSED("closed"),
    SUSPENDED("suspend"),
    BLOCKED("blocked")
}