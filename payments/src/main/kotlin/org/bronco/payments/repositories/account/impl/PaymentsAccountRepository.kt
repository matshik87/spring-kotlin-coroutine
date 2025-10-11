package org.bronco.payments.repositories.account.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import org.bronco.payments.model.Currencies
import org.bronco.payments.model.ResourceCreationException
import org.bronco.payments.model.ResourceType
import org.bronco.payments.repositories.account.AccountRepository
import org.bronco.payments.repositories.account.model.AccountData
import org.bronco.payments.repositories.account.model.AccountStatus
import org.bronco.payments.schema.jooq.model.tables.Account
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
open class PaymentsAccountRepository(
    private val context: DSLContext
) : AccountRepository {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private fun table(): Account = Account.ACCOUNT

    override suspend fun createNewAccount(customerId: UUID): AccountData = coroutineScope {
        coroutineScope {
            context.transactionCoroutine { transactional ->
                val transaction = DSL.using(transactional)
                val table = table()
                val accountRecord = transaction.newRecord(table).apply {
                    id = UUID.randomUUID()
                    currencyCode = Currencies.USD.name
                    status = AccountStatus.INACTIVE.name
                    balance = BigDecimal.ZERO.setScale(2)
                    creationDate = LocalDateTime.now()
                    customerReference = customerId
                }
                transaction.batchInsert(accountRecord).executeAsync()
                    .handleAsync { results, throwable ->
                        if (throwable != null || results.first() != 1) {
                            logger.error("An account could not be created for customer $customerId", throwable)
                            throw ResourceCreationException(ResourceType.ACCOUNT)
                        }
                        logger.info("An account was successfully created for customer ${customerId}")
                        accountRecord.into(AccountData::class.java)
                    }.await()
            }
        }
    }

    override suspend fun getCustomerAccountsByStatus(
        customerId: UUID,
        statuses: Set<AccountStatus>
    ): List<AccountData> =
        coroutineScope {
            context.transactionCoroutine { transactional ->
                val transaction = DSL.using(transactional)
                val table = table()
                transaction.selectFrom(table)
                    .where(
                        table.CUSTOMER_REFERENCE.eq(customerId)
                            .and(table.STATUS.`in`(statuses))
                    )
                    .fetchAsync()
                    .await()
                    .into(AccountData::class.java)
            }
        }

    override suspend fun getAllCustomerAccounts(customerId: UUID): List<AccountData> =
        coroutineScope {
            context.transactionCoroutine { transactional ->
                val transaction = DSL.using(transactional)
                val table = table()
                transaction.selectFrom(table)
                    .where(table.CUSTOMER_REFERENCE.eq(customerId))
                    .fetchAsync()
                    .await()
                    .into(AccountData::class.java)
            }
        }
}