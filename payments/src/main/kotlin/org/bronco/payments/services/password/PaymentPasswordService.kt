package org.bronco.payments.services.password

import at.favre.lib.crypto.bcrypt.BCrypt
import org.bronco.payments.config.properties.PasswordProperties
import org.springframework.stereotype.Service

@Service
class PaymentPasswordService(
    private val passwordProperties: PasswordProperties
) : PasswordService {

    override fun encode(password: String?): String {
        val passwordToEncode = password ?: passwordProperties.default
        return BCrypt.withDefaults().hashToString(passwordProperties.cost, passwordToEncode.toCharArray())
    }
}