package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.utils.CountryUtils
import org.bronco.payments.validation.customer.ValidNationality

class NationalityValidator : ConstraintValidator<ValidNationality, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean = CountryUtils.isCountrySupported(value)
}