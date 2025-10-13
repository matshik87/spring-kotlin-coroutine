package org.bronco.payments.services.login

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.absoluteValue

@Service
class PaymentsLoginService : LoginService {
    override fun generateLogin(string: () -> String): String {
        val seedNano = LocalDateTime.now().nano
        val seedString = string().hashCode().absoluteValue

        return (seedNano - seedString).absoluteValue.toString()
    }
}