package org.bronco.payments.utils

import jakarta.validation.ConstraintValidatorContext

fun ConstraintValidatorContext?.resetDefaultContext(template: String, populateAsTemplate: Boolean = true): Unit {
    this?.disableDefaultConstraintViolation()
    val constraintViolationTemplate = if (populateAsTemplate) {
        "{${template}}"
    } else template
    this?.buildConstraintViolationWithTemplate(constraintViolationTemplate)?.addConstraintViolation()
}