package org.bronco.payments.services.processes.model

enum class ProgressType(val terminal: Boolean) {
    INITIALIZED(false),
    IN_PROGRESS(false),
    FINISHED(true),
    FINISHED_WITH_ERROR(true),
    ALREADY_PROCESSED(true)
}