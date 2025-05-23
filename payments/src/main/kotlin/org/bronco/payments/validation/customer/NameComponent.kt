package org.bronco.payments.validation.customer

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Constraint(validatedBy = [NameComponentValidation::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NameComponent(
    val type: NameComponentType,
    val message: String = "{name.component.invalid}",
    val min: Int = 2,
    val max: Int = 40,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)