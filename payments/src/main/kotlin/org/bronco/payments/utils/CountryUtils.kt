package org.bronco.payments.utils

import java.util.Locale
import kotlin.streams.asSequence

object CountryUtils {
    private val COUNTRY_CODES: Set<String> = Locale.availableLocales().asSequence()
        .map { locale -> locale.country }
        .filter { countryCode -> countryCode.isNotBlank() || !countryCode.all { it.isDigit() } }
        .toSet()

    fun isCountrySupported(countryCode: String?): Boolean = COUNTRY_CODES.contains(countryCode)
}