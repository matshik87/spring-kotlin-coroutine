package org.bronco.payments.services.password

import org.bronco.payments.config.properties.PasswordProperties
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class PaymentPasswordService(
    private val passwordProperties: PasswordProperties
) : PasswordService {
    private val messageDigest: MessageDigest = MessageDigest.getInstance(passwordProperties.algorithm)

    override fun encode(password: String?): String {
        val passwordToEncode = password ?: passwordProperties.default
        val encoded = messageDigest.digest(passwordToEncode.toByteArray(passwordProperties.charset))
        return String(encoded)
    }
}