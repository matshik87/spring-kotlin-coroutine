package org.bronco.payments.repositories.progress.impl

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.EnumerablePropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import java.util.concurrent.atomic.AtomicBoolean

private const val defaultPostgresImage = "postgres:13-alpine"

open class PostgreSqlInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
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


    private fun createContainer(
        dockerImage: String,
        user: String,
        password: String,
        databaseName: String,
        initScript: String? = null
    ): PostgreSQLContainer<*> = PostgreSQLContainer(dockerImage)
        .apply {
            withExposedPorts(defaultExposedPort)
                .withUsername(user)
                .withPassword(password)
                .withDatabaseName(databaseName)
                .withCommand()
                .withReuse(true)
            if (initScript != null) {
                withCopyFileToContainer(
                    MountableFile.forHostPath(initScript),
                    "/docker-entrypoint-initdb.d/init.sql"
                )
            }
            start()
        }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment = applicationContext.environment
        val propertySources = environment.propertySources
        val runContainer = environment.getProperty(isEnabled, defaultBooleanValue).toBooleanStrict()
        if (runContainer && !containerCreated.get()) {
            val container = createContainer(
                dockerImage = environment.getProperty(dockerImage, defaultPostgresImage),
                user = environment.getProperty(username, defaultValue),
                password = environment.getProperty(password, defaultValue),
                databaseName = environment.getProperty(databaseName, defaultValue),
                initScript = environment.getProperty(initScriptProperty)
                    ?.let { resourceName -> applicationContext.getResource(resourceName).file.path }
            )
            containerCreated.set(true)
            if (environment.getProperty(destroyOnExit, defaultBooleanValue).toBooleanStrict()) {
                Runtime.getRuntime().addShutdownHook(Thread(container::stop))
            }
            val scopedValue = ScopedValue.where(postgresContainer, container)

            val databaseName = { scopedValue.get(postgresContainer).databaseName }
            val properties = buildMap {
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
            propertySources.addLast(PostgresContainerPropertySource(properties))
        }
    }

    private class PostgresContainerPropertySource(source: Map<String, () -> String>) :
        EnumerablePropertySource<Map<String, () -> String>>("postgresPropertySource", source) {
        override fun getProperty(name: String): Any? = if (source.containsKey(name)) {
            source[name]!!()
        } else null

        override fun getPropertyNames(): Array<String> = source.keys.toTypedArray()
        override fun containsProperty(name: String): Boolean = source.containsKey(name)
    }
}