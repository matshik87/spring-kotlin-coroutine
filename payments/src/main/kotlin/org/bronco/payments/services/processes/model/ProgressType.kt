package org.bronco.payments.services.processes.model

enum class ProgressType(val terminal: Boolean = false) {
    INITIALIZED(),
    DISPATCHED(),
    IN_PROGRESS(),
    FINISHED(true),
    FINISHED_WITH_ERROR(true),
    ALREADY_PROCESSED(true)
    ;

    companion object {
        fun fromString(value: String?): ProgressType = ProgressType.entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Unknown progress type was received: $value")
    }
}