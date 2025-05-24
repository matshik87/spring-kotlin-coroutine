package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.utils.DateUtils.convertStringToLocalDate
import org.bronco.payments.validation.customer.IsAdult
import java.time.LocalDate

class AdultValidation : ConstraintValidator<IsAdult, String> {
    companion object {
        private const val NO_DOB_ERROR = "{age.empty}"
        private const val MIN_AGE = 18L
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        return convertStringToLocalDate(value)?.let { date ->
            val adultAge = LocalDate.now().minusYears(MIN_AGE)
            adultAge == date || date.isBefore(LocalDate.now().minusYears(MIN_AGE))
        } ?: run {
            context?.apply {
                disableDefaultConstraintViolation()
                buildConstraintViolationWithTemplate(NO_DOB_ERROR).addConstraintViolation()
            }
            false
        }
    }
}