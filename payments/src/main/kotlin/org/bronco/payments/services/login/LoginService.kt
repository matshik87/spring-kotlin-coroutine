package org.bronco.payments.services.login

interface LoginService {
    fun generateLogin(string: () -> String): String
}