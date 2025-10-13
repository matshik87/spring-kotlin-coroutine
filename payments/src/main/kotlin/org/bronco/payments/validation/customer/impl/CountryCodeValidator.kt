package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.utils.CountryUtils
import org.bronco.payments.validation.customer.CountryCode

class CountryCodeValidator : ConstraintValidator<CountryCode, String> {
    private var optional: Boolean? = null
    override fun initialize(constraintAnnotation: CountryCode?) {
        this.optional = constraintAnnotation?.optional
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        val result = CountryUtils.isCountrySupported(value)
        return optional?.let { optionalValue ->
            if (optionalValue) result || value.isNullOrBlank() else result
        } ?: true
    }
}