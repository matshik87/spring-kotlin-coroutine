package org.bronco.payments.initializers.base

data class ContainerProperties(
    val dockerImage: String,
    val jdbcProperties: JdbcProperties?
) {
}

data class JdbcProperties(
    val user: String,
    val password: String,
    val databaseName: String,
    val defaultExposedPort: Int?,
    val initScript: String? = null
)