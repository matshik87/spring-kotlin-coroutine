package org.bronco.payments.schema.jooq.model.converters

import org.bronco.payments.repositories.account.model.AccountStatus
import org.jooq.Converter

class AccountStatusConverter : Converter<String, AccountStatus> {
    override fun from(databaseObject: String?): AccountStatus? = AccountStatus.entries.firstOrNull { instance ->
        instance.name == databaseObject
    }

    override fun to(userObject: AccountStatus?): String? = userObject?.name

    override fun fromType(): Class<String> = String::class.java

    override fun toType(): Class<AccountStatus> = AccountStatus::class.java
}