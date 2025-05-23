package org.bronco.payments.controllers.api

import com.fasterxml.jackson.annotation.JsonFormat
import org.bronco.payments.validation.customer.NameComponent
import org.bronco.payments.validation.customer.NameComponentType
import java.time.LocalDate

//TODO: add request validation
class CreateCustomerRequest(
    @NameComponent(type = NameComponentType.FIRST_NAME)
    val firstName: String,
    @NameComponent(type = NameComponentType.MIDDLE_NAME)
    val middleName: String?,
    @NameComponent(type = NameComponentType.LAST_NAME,max = 60)
    val lastName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    val dateOfBirth: LocalDate,
    val nationality: String, //most probably an enum
    val password: String?,
    val email: String,
    val phoneNumber: String,
    val secondaryPhoneNumber: String?
)