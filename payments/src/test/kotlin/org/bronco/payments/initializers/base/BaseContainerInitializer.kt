package org.bronco.payments.initializers.base

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.MutablePropertySources
import java.util.concurrent.atomic.AtomicReference

abstract class BaseContainerInitializer<T>(
    private val propertySourceName: String
) : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val containerReference: AtomicReference<T> = AtomicReference(null)

    protected abstract fun createContainer(): (ContainerProperties) -> T

    protected fun bindPropertySource(
        propertySources: MutablePropertySources,
        properties: () -> Map<String, () -> String>
    ) {
        propertySources.addLast(ContainerPropertySource(propertySourceName, properties()))
    }

    protected fun getContainer(containerPropertiesGenerator: () -> ContainerProperties): T {
        return containerReference.get() ?: run {
            containerReference.set(createContainer()(containerPropertiesGenerator()))
            containerReference.get()
        }
    }

    protected class ContainerPropertySource(propertySourceName: String, source: Map<String, () -> String>) :
        EnumerablePropertySource<Map<String, () -> String>>(propertySourceName, source) {
        override fun getProperty(name: String): Any? = source[name]?.let { it() }
        override fun getPropertyNames(): Array<String> = source.keys.toTypedArray()
        override fun containsProperty(name: String): Boolean = source.containsKey(name)
    }
}