package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.utils.CountryUtils
import org.bronco.payments.validation.customer.ValidNationality

class NationalityValidator : ConstraintValidator<ValidNationality, String> {
    override fun isValid(p0: String?, p1: ConstraintValidatorContext?): Boolean = CountryUtils.isCountrySupported(p0)
}