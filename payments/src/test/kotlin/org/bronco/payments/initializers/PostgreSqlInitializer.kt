package org.bronco.payments.initializers

import org.bronco.payments.initializers.base.BaseContainerInitializer
import org.bronco.payments.initializers.base.ContainerProperties
import org.bronco.payments.initializers.base.JdbcProperties
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import java.util.concurrent.atomic.AtomicBoolean

private const val defaultPostgresImage = "postgres:13-alpine"

open class PostgreSqlInitializer : BaseContainerInitializer<PostgreSQLContainer<*>>("postgresPropertySource") {
    companion object {
        private val defaultExposedPort = 5432
        private val defaultBooleanValue = "false"
        private val defaultValue = "test"
        private val isEnabled = "containers.db.enabled"
        private val dockerImage = "containers.db.image"
        private val username = "containers.db.user"
        private val password = "containers.db.password"
        private val databaseName = "containers.db.name"
        private val destroyOnExit = "containers.db.destroy-on-exit"
        private val databaseNameProperty = "containers.db.properties.databaseName"
        private val databaseUrlProperty = "containers.db.properties.databaseUrl"
        private val usernameProperty = "containers.db.properties.username"
        private val passwordProperty = "containers.db.properties.password"
        private val initScriptProperty = "containers.db.properties.init-script"
        private val postgresContainer: ScopedValue<PostgreSQLContainer<*>> = ScopedValue.newInstance()
        private val containerCreated = AtomicBoolean(false)
    }

    override fun createContainer(): (ContainerProperties) -> PostgreSQLContainer<*> =
        { properties ->
            PostgreSQLContainer(properties.dockerImage)
                .apply {
                    withExposedPorts(properties.jdbcProperties!!.defaultExposedPort)
                        .withUsername(properties.jdbcProperties!!.user)
                        .withPassword(properties.jdbcProperties!!.password)
                        .withDatabaseName(properties.jdbcProperties!!.databaseName)
                        .withCommand()
                        .withReuse(true)
                    val initScript = properties.jdbcProperties.initScript
                    if (initScript != null) {
                        withCopyFileToContainer(
                            MountableFile.forHostPath(initScript),
                            "/docker-entrypoint-initdb.d/init.sql"
                        )
                    }
                    start()
                }
        }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment = applicationContext.environment
        val propertySources = environment.propertySources
        val runContainer = environment.getProperty(isEnabled, defaultBooleanValue).toBooleanStrict()
        if (runContainer && !containerCreated.get()) {
            val container = createContainer()(
                ContainerProperties(
                    dockerImage = environment.getProperty(dockerImage, defaultPostgresImage),
                    jdbcProperties = JdbcProperties(
                        user = environment.getProperty(username, defaultValue),
                        password = environment.getProperty(password, defaultValue),
                        databaseName = environment.getProperty(databaseName, defaultValue),
                        defaultExposedPort = defaultExposedPort,
                        initScript = environment.getProperty(initScriptProperty)
                            ?.let { resourceName -> applicationContext.getResource(resourceName).file.path }
                    )
                )
            )
            containerCreated.set(true)
            if (environment.getProperty(destroyOnExit, defaultBooleanValue).toBooleanStrict()) {
                Runtime.getRuntime().addShutdownHook(Thread(container::stop))
            }
            val scopedValue = ScopedValue.where(postgresContainer, container)

            bindPropertySource(propertySources) {
                val databaseName = { scopedValue.get(postgresContainer).databaseName }
                buildMap {
                    environment.getProperty(databaseNameProperty, defaultValue).split(',').map { it.trim() }
                        .map { key -> key to databaseName }.forEach { entry -> put(entry.first, entry.second) }
                    put(
                        environment.getProperty(databaseUrlProperty, defaultValue),
                        { scopedValue.get(postgresContainer).getJdbcUrl() })
                    put(
                        environment.getProperty(usernameProperty, defaultValue),
                        { scopedValue.get(postgresContainer).username })
                    put(
                        environment.getProperty(passwordProperty, defaultValue),
                        { scopedValue.get(postgresContainer).password })
                }
            }
        }
    }
}