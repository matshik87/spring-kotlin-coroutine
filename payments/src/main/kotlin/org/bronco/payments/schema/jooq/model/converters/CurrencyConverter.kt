package org.bronco.payments.schema.jooq.model.converters

import org.bronco.payments.model.Currencies
import org.jooq.Converter

class CurrencyConverter : Converter<String, Currencies> {
    override fun from(databaseObject: String?): Currencies? = Currencies.entries.firstOrNull { instance ->
        instance.name == databaseObject
    }

    override fun to(userObject: Currencies?): String? = userObject?.name

    override fun fromType(): Class<String> = String::class.java

    override fun toType(): Class<Currencies> = Currencies::class.java
}