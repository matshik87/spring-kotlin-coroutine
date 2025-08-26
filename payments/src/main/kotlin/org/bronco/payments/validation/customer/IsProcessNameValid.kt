package org.bronco.payments.validation.customer

import jakarta.validation.Constraint
import jakarta.validation.Payload
import org.bronco.payments.validation.customer.impl.ProcessNameValidation
import kotlin.reflect.KClass

@Constraint(validatedBy = [ProcessNameValidation::class])
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsProcessNameValid(
    val message: String = "{process.name.invalid}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
