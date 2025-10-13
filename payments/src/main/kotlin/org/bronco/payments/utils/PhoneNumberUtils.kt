package org.bronco.payments.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumberUtils {
    private val PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance()

    fun isValidPhoneNumber(phoneNumber: String, countryCode: String): Boolean {
        return kotlin.runCatching {
            val phoneNumberInstance = PHONE_NUMBER_UTIL.parse(phoneNumber, countryCode)
            PHONE_NUMBER_UTIL.isValidNumber(phoneNumberInstance)
        }.getOrElse { false }
    }
}