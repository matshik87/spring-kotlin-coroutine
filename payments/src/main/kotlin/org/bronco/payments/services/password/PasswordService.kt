package org.bronco.payments.services.password

interface PasswordService {
    fun encode(password: String?): String
}