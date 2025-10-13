package org.bronco.payments.validation.customer

import jakarta.validation.Constraint
import jakarta.validation.Payload
import org.bronco.payments.validation.customer.impl.PhoneNumberValidator
import kotlin.reflect.KClass

@Constraint(validatedBy = [PhoneNumberValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidPhoneNumber(
    val message: String = "{phone.primary.invalid}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
