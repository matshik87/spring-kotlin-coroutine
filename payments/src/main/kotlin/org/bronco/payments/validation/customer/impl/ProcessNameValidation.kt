package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.services.processes.model.ProcessName
import org.bronco.payments.validation.customer.IsProcessNameValid

class ProcessNameValidation : ConstraintValidator<IsProcessNameValid, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        ProcessName.entries.firstOrNull { instance -> instance.name == value } != null
}