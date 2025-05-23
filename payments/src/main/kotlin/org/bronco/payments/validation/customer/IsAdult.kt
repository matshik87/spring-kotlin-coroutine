package org.bronco.payments.validation.customer

import jakarta.validation.Constraint
import jakarta.validation.Payload
import org.bronco.payments.validation.customer.impl.AdultValidation
import kotlin.reflect.KClass

@Constraint(validatedBy = [AdultValidation::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsAdult(
    val message: String = "{age.invalid}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
