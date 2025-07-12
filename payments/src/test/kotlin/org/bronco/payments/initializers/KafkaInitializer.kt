package org.bronco.payments.initializers

import org.bronco.payments.initializers.base.BaseContainerInitializer
import org.bronco.payments.initializers.base.ContainerProperties
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.kafka.KafkaContainer

open class KafkaInitializer : BaseContainerInitializer<KafkaContainer>("kafkaPropertySource") {
    companion object {
        private val defaultKafkaImage = "apache/kafka:3.9.1"
        private val defaultBooleanValue = "false"
        private val defaultValue = "test"
        private val isEnabled = "containers.kafka.enabled"
        private val dockerImage = "containers.kafka.image"
        private val destroyOnExit = "containers.kafka.destroy-on-exit"
        private val bootstrapServersNameProperty = "containers.kafka.properties.bootstrap-servers"
    }

    override fun createContainer(): (ContainerProperties) -> KafkaContainer = { properties ->
        KafkaContainer(properties.dockerImage)
            .apply {
                withReuse(true)
                start()
            }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment = applicationContext.environment
        val propertySources = environment.propertySources
        val runContainer = environment.getProperty(isEnabled, defaultBooleanValue).toBooleanStrict()
        if (runContainer) {
            val container = getContainer {
                ContainerProperties(environment.getProperty(dockerImage, defaultKafkaImage), jdbcProperties = null)
            }
            if (environment.getProperty(destroyOnExit, defaultBooleanValue).toBooleanStrict()) {
                Runtime.getRuntime().addShutdownHook(Thread(container::stop))
            }
            val bootstrapServers = { container.bootstrapServers }
            bindPropertySource(propertySources) {
                buildMap {
                    environment.getProperty(bootstrapServersNameProperty, defaultValue)
                        .split(',').map { it.trim() }
                        .map { key -> key to bootstrapServers }.forEach { entry -> put(entry.first, entry.second) }
                }
            }
        }
    }
}