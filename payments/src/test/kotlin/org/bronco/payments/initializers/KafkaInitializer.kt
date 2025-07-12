package org.bronco.payments.initializers

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.EnumerablePropertySource
import org.testcontainers.kafka.KafkaContainer
import java.util.concurrent.atomic.AtomicBoolean

open class KafkaInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        private val defaultKafkaImage = "apache/kafka:3.9.1"
        private val defaultBooleanValue = "false"
        private val defaultValue = "test"
        private val isEnabled = "containers.kafka.enabled"
        private val dockerImage = "containers.kafka.image"
        private val destroyOnExit = "containers.kafka.destroy-on-exit"
        private val bootstrapServersNameProperty = "containers.kafka.properties.bootstrap-servers"
        private val kafkaContainer: ScopedValue<KafkaContainer> = ScopedValue.newInstance()
        private val containerCreated = AtomicBoolean(false)
    }


    private fun createContainer(
        dockerImage: String,
    ): KafkaContainer = KafkaContainer(dockerImage)
        .apply {
            withReuse(true)
            start()
        }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment = applicationContext.environment
        val propertySources = environment.propertySources
        val runContainer = environment.getProperty(isEnabled, defaultBooleanValue).toBooleanStrict()
        if (runContainer && !containerCreated.get()) {
            val container = createContainer(
                dockerImage = environment.getProperty(dockerImage, defaultKafkaImage),
            )
            containerCreated.set(true)
            if (environment.getProperty(destroyOnExit, defaultBooleanValue).toBooleanStrict()) {
                Runtime.getRuntime().addShutdownHook(Thread(container::stop))
            }
            val scopedValue = ScopedValue.where(kafkaContainer, container)

            val bootstrapServers = { scopedValue.get(kafkaContainer).bootstrapServers }
            val properties = buildMap {
                environment.getProperty(bootstrapServersNameProperty, defaultValue)
                    .split(',').map { it.trim() }
                    .map { key -> key to bootstrapServers }.forEach { entry -> put(entry.first, entry.second) }
            }
            propertySources.addLast(KafkaContainerPropertySource(properties))
        }
    }

    private class KafkaContainerPropertySource(source: Map<String, () -> String>) :
        EnumerablePropertySource<Map<String, () -> String>>("kafkaPropertySource", source) {
        override fun getProperty(name: String): Any? = if (source.containsKey(name)) {
            source[name]!!()
        } else null

        override fun getPropertyNames(): Array<String> = source.keys.toTypedArray()
        override fun containsProperty(name: String): Boolean = source.containsKey(name)
    }
}