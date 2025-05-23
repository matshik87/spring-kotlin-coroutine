package org.bronco.payments.repositories

import jakarta.persistence.Column
import java.time.LocalDate
import java.util.*

data class CustomerData(
    @Column(name = "id")
    val customerId: UUID?,
    @Column(name = "first_name")
    val firstName: String?,
    @Column(name = "middle_name")
    val middleName: String?,
    @Column(name = "last_name")
    val lastName: String?,
    @Column(name = "dob")
    val dateOfBirth: LocalDate?,
    @Column(name = "nationality")
    val nationality: String?,
    @Column(name = "login")
    val login: String,
    @Column(name = "password")
    val password: String?, //should be nullable
    @Column(name = "email")
    val email: String,
    @Column(name = "phone_number")
    val phoneNumber: String?,
    @Column(name = "secondary_phone_number")
    val secondaryPhoneNumber: String? = null,
    val errorMessage: String? = null,
    val passwordChangeRequired: Boolean? = null
) {}
