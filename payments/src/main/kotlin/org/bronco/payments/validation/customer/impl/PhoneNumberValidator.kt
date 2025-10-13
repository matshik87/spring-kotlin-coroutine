package org.bronco.payments.validation.customer.impl

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.utils.PhoneNumberUtils
import org.bronco.payments.utils.resetDefaultContext
import org.bronco.payments.validation.customer.ValidPhoneNumber

class PhoneNumberValidator : ConstraintValidator<ValidPhoneNumber, CreateCustomerRequest> {
    companion object {
        private val PRIMARY_PHONE_INVALID_TEMPLATE = "phone.primary.invalid"
        private val SECONDARY_PHONE_INVALID_TEMPLATE = "phone.secondary.invalid"
    }

    override fun isValid(value: CreateCustomerRequest?, context: ConstraintValidatorContext?): Boolean {
        return value?.let { request ->
            val countryCode = if (request.countryOfResidence.isNullOrBlank()) value.nationality else request.countryOfResidence
            booleanArrayOf(
                validatePhoneNumber(
                    request.phoneNumber,
                    countryCode,
                    context,
                    template = PRIMARY_PHONE_INVALID_TEMPLATE
                ), validatePhoneNumber(
                    request.secondaryPhoneNumber,
                    countryCode,
                    context,
                    optional = true,
                    template = SECONDARY_PHONE_INVALID_TEMPLATE
                )
            ).all { it }
        } ?: false
    }

    private fun validatePhoneNumber(
        phoneNumber: String?,
        countryCode: String,
        context: ConstraintValidatorContext?,
        optional: Boolean = false,
        template: String? = null
    ): Boolean {
        return when {
            optional && phoneNumber.isNullOrBlank() -> true
            !optional && phoneNumber.isNullOrBlank() -> false
            else -> {
                phoneNumber?.let { phone ->
                    val isValid = PhoneNumberUtils.isValidPhoneNumber(phone, countryCode)
                    if (!isValid) {
                        template?.let {
                            context.resetDefaultContext(template)
                        }
                    }
                    isValid
                } ?: false
            }
        }
    }
}