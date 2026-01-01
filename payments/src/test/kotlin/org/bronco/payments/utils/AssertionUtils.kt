package org.bronco.payments.utils

import io.mockk.MockKVerificationScope
import org.assertj.core.api.Assertions.assertThat
import org.bronco.payments.repositories.progress.ProcessProgressProperties
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.services.processes.model.ProgressType
import java.util.*

object AssertionUtils {
    fun MockKVerificationScope.assertProgressProperties(
        processType: ProcessType,
        progressType: ProgressType = ProgressType.IN_PROGRESS,
        parentProcessId: UUID?= null,
        entityId: UUID? = null,
        errorMessage: String? = null
    ): ProcessProgressProperties =
        coWithArg { properties ->
            assertThat(properties).isNotNull
                .returns(processType) { it.processType }
                .returns(progressType) { it.progressType }
                .returns(parentProcessId) { it.parentProcessId }
                .returns(entityId) { it.entityId }
                .returns(errorMessage) { it.progressDetails }
            assertThat(properties.id).isNotNull()
        }
}