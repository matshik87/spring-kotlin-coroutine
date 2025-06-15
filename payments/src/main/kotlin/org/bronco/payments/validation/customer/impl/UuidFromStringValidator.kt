package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.utils.toUuid
import org.bronco.payments.validation.customer.ValidUuid

internal class UuidFromStringValidator : ConstraintValidator<ValidUuid, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean =
        if (value.isNullOrBlank()) false else try {
            value.toUuid()
            true
        } catch (e: IllegalArgumentException) {
            false
        }
}