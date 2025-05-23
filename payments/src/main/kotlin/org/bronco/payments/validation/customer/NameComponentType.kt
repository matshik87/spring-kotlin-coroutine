package org.bronco.payments.validation.customer

enum class NameComponentType(val propertyName: String, val optional: Boolean) {
    FIRST_NAME("firstName", false),
    MIDDLE_NAME("middleName", true),
    LAST_NAME("lastName", false)
}