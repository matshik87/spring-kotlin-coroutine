package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.services.processes.model.ProcessType
import org.bronco.payments.validation.customer.IsProcessTypeValid

class ProcessTypeValidation : ConstraintValidator<IsProcessTypeValid, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        ProcessType.entries.firstOrNull { instance -> instance.name == value } != null
}