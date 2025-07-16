package org.bronco.payments.utils

import kotlinx.coroutines.coroutineScope
import org.apache.commons.lang3.RandomStringUtils
import org.bronco.payments.controllers.api.CreateCustomerRequest
import org.bronco.payments.repositories.customer.CustomerData
import org.bronco.payments.utils.TemporalUtils.LOCAL_DATE_FORMATTER
import java.time.LocalDate
import java.util.*

object CustomerDataGenerators {
    suspend fun generateCustomerData(customerId: UUID? = null): CustomerData = coroutineScope {
        val randomStringUtils = RandomStringUtils.secure()
        val nationality = "UK"
        CustomerData(
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
            secondaryPhoneNumber = "+${randomStringUtils.nextNumeric(8)}"
        )
    }

    suspend fun generateCreateCustomerRequest(
        email: String
    ): CreateCustomerRequest = coroutineScope {
        val randomStringUtils = RandomStringUtils.secure()
        val nationality = "UK"
        CreateCustomerRequest(
            firstName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            middleName = null,
            lastName = randomStringUtils.nextAscii(10)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            dateOfBirth = LOCAL_DATE_FORMATTER.format(LocalDate.now().minusYears(18).minusDays(1)),
            nationality = nationality,
            countryOfResidence = nationality,
            password = randomStringUtils.nextAlphabetic(15),
            email = email,
            phoneNumber = "+${randomStringUtils.nextNumeric(8)}",
            secondaryPhoneNumber = "+${randomStringUtils.nextNumeric(8)}"
        )
    }
}