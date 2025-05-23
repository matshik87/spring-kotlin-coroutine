package org.bronco.payments.validation.customer

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class NameComponentValidation : ConstraintValidator<NameComponent, String> {
    private lateinit var nameComponentType: NameComponentType
    private var minLength: Int = 0
    private var maxLength: Int = 0
    override fun initialize(annotation: NameComponent?) {
        annotation?.let { annotated ->
            nameComponentType = annotated.type
            minLength = annotated.min
            maxLength = annotated.max
        }
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return if (nameComponentType.optional && value.isNullOrBlank()) {
            true
        } else {
            when {
                value.isNullOrBlank() -> {
                    context.resetDefaultViolation("name.${nameComponentType.propertyName}.empty")
                    false
                }

                !value.isLengthInRange(minLength, maxLength) -> {
                    context.resetDefaultViolation("name.${nameComponentType.propertyName}.length")
                    false
                }

                !value.isCamelCase() -> {
                    context.disableDefaultConstraintViolation()
                    context.resetDefaultViolation("name.${nameComponentType.propertyName}.invalid")
                    false
                }

                else -> true
            }
        }
    }

    private fun String?.isCamelCase(): Boolean = this?.let { value ->
        value.trim() == value && value.first().isUpperCase()
    } ?: false

    private fun String?.isLengthInRange(minLength: Int, maxLength: Int): Boolean = this?.let { value ->
        value.length in minLength..maxLength
    } ?: false

    private fun ConstraintValidatorContext.resetDefaultViolation(template: String) {
        this.disableDefaultConstraintViolation()
        this.buildConstraintViolationWithTemplate("{${template}}").addConstraintViolation()
    }
}