package org.bronco.payments.utils

import org.apache.commons.lang3.RandomStringUtils
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.controllers.api.RetrieveCustomerResponse
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.utils.TemporalUtils.LOCAL_DATE_FORMATTER
import java.time.LocalDate
import java.util.*

object CustomerDataGenerators {
    fun generateCustomerData(customerId: UUID? = null, errorMessage: String? = null): CustomerData {
        val randomStringUtils = RandomStringUtils.secure()
        val nationality = "UK"

        return CustomerData(
            customerId = customerId,
            firstName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            middleName = null,
            lastName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            dateOfBirth = LocalDate.now().minusYears(18).minusDays(1),
            nationality = nationality,
            countryOfResidence = nationality,
            login = randomStringUtils.nextAscii(10),
            password = randomStringUtils.nextAlphabetic(15),
            email = "${randomStringUtils.nextAscii(5, 10)}@test.com",
            phoneNumber = "+${randomStringUtils.nextNumeric(8)}",
            secondaryPhoneNumber = "+${randomStringUtils.nextNumeric(8)}",
            errorMessage = errorMessage
        )
    }

    fun generateCreateCustomerRequest(
        email: String = "test@test.com",
        nationality: String = "GB",
        firstName: String? = null,
        middleName: String? = null,
        birthDay: String? = null,
        countryOfResidence: String? = nationality,
        phoneNumber: String? = null
    ): CreateCustomerRequest {
        val randomStringUtils = RandomStringUtils.secure()
        return CreateCustomerRequest(
            firstName = firstName ?: randomStringUtils.nextAlphabetic(10).lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            middleName = middleName,
            lastName = randomStringUtils.nextAlphabetic(10).lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            dateOfBirth = birthDay ?: LOCAL_DATE_FORMATTER.format(LocalDate.now().minusYears(18).minusDays(1)),
            nationality = nationality,
            countryOfResidence = countryOfResidence,
            password = randomStringUtils.nextAlphabetic(15),
            email = email,
            phoneNumber = phoneNumber ?: "+44207123${randomStringUtils.nextNumeric(4)}",
            secondaryPhoneNumber = "+44207123${randomStringUtils.nextNumeric(4)}"
        )
    }

    fun generateRetrieveCustomerResponse(customerId: UUID): RetrieveCustomerResponse {
        val customerData = generateCustomerData(customerId)
        return RetrieveCustomerResponse(
            customerId = customerId,
            firstName = customerData.firstName,
            middleName = customerData.middleName,
            lastName = customerData.lastName,
            dateOfBirth = customerData.dateOfBirth,
            nationality = customerData.nationality,
            countryOfResidence = customerData.countryOfResidence,
            email = customerData.email,
            phoneNumber = customerData.phoneNumber,
            secondaryPhoneNumber = customerData.secondaryPhoneNumber,
            login = customerData.login,
            passwordChangeRequired = customerData.passwordChangeRequired
        )
    }
}