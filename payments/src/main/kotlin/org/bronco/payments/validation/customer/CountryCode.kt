package org.bronco.payments.validation.customer

import jakarta.validation.Constraint
import jakarta.validation.Payload
import org.bronco.payments.validation.customer.impl.CountryCodeValidator
import kotlin.reflect.KClass

@Constraint(validatedBy = [CountryCodeValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CountryCode(
    val message: String = "{country.code.invalid}",
    val optional: Boolean = true,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
