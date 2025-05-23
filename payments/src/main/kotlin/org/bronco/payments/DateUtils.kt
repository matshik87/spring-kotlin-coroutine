package org.bronco.payments

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    val LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun convertStringToLocalDate(date: String?): LocalDate? =
        date?.let {
            runCatching { LocalDate.parse(date, LOCAL_DATE_FORMATTER) }.getOrNull()
        }
}