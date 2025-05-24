package org.bronco.payments.controllers.api

import jakarta.validation.constraints.Email
import org.bronco.payments.validation.customer.*

@ValidPhoneNumber
class CreateCustomerRequest(
    @NameComponent(type = NameComponentType.FIRST_NAME)
    val firstName: String,
    @NameComponent(type = NameComponentType.MIDDLE_NAME)
    val middleName: String?,
    @NameComponent(type = NameComponentType.LAST_NAME, max = 60)
    val lastName: String,
    @IsAdult
    val dateOfBirth: String,
    @ValidNationality
    val nationality: String,
    val password: String?,
    @field:Email(message = "{email.invalid}")
    val email: String,
    val phoneNumber: String,
    val secondaryPhoneNumber: String?
)