package org.bronco.payments.model

enum class Currencies {
    USD
    ;

    companion object {
        fun of(currency: String): Currencies = entries.firstOrNull { it.name == currency }
            ?: throw IllegalArgumentException("Currency $currency not found")
    }
}