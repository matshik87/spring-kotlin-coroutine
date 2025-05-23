package org.bronco.payments.controllers.api

import org.bronco.payments.validation.customer.IsAdult
import org.bronco.payments.validation.customer.NameComponent
import org.bronco.payments.validation.customer.NameComponentType

//TODO: add request validation
class CreateCustomerRequest(
    @NameComponent(type = NameComponentType.FIRST_NAME)
    val firstName: String,
    @NameComponent(type = NameComponentType.MIDDLE_NAME)
    val middleName: String?,
    @NameComponent(type = NameComponentType.LAST_NAME,max = 60)
    val lastName: String,
    @IsAdult
    val dateOfBirth: String,
    val nationality: String, //most probably an enum
    val password: String?,
    val email: String,
    val phoneNumber: String,
    val secondaryPhoneNumber: String?
)