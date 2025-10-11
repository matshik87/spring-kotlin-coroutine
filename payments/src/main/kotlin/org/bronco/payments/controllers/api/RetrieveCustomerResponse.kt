package org.bronco.payments.controllers.api

import com.fasterxml.jackson.annotation.JsonFormat
import org.bronco.payments.repositories.account.model.AccountDto
import java.time.LocalDate
import java.util.*

data class RetrieveCustomerResponse(
    val customerId: UUID?,
    val firstName: String?,
    val middleName: String?,
    val lastName: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dateOfBirth: LocalDate?,
    val nationality: String?,
    val countryOfResidence: String,
    val login: String,
    val email: String,
    val phoneNumber: String?,
    val secondaryPhoneNumber: String? = null,
    val passwordChangeRequired: Boolean? = null,
    val accounts: List<AccountDto> = emptyList()
)
