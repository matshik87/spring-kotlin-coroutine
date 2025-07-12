package org.bronco.payments.initializers.base

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.MutablePropertySources

abstract class BaseContainerInitializer<T>(
    private val propertySourceName: String
) : ApplicationContextInitializer<ConfigurableApplicationContext> {
    protected abstract fun createContainer(): (ContainerProperties) -> T

    protected fun bindPropertySource(
        propertySources: MutablePropertySources,
        properties: () -> Map<String, () -> String>
    ) {
        propertySources.addLast(ContainerPropertySource(propertySourceName, properties()))
    }

    protected class ContainerPropertySource(propertySourceName: String, source: Map<String, () -> String>) :
        EnumerablePropertySource<Map<String, () -> String>>(propertySourceName, source) {

        override fun getProperty(name: String): Any? = source[name]?.let { it() }
        override fun getPropertyNames(): Array<String> = source.keys.toTypedArray()
        override fun containsProperty(name: String): Boolean = source.containsKey(name)
    }
}