package org.bronco.payments.validation.customer

import jakarta.validation.Constraint
import jakarta.validation.Payload
import org.bronco.payments.validation.customer.impl.ProcessTypeValidation
import kotlin.reflect.KClass

@Constraint(validatedBy = [ProcessTypeValidation::class])
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsProcessTypeValid(
    val message: String = "{process.name.invalid}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
