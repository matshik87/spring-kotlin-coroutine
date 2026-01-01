package org.bronco.payments.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.bronco.payments.config.conditionals.KafkaLocal
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.bronco.payments.config.properties.KafkaComponentProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*

@EnableKafka
@Configuration
open class KafkaConfiguration {
    companion object {
        const val createCustomerTopic = "createCustomer"
        const val createCustomerAccountTopic = "createCustomerAccount"
    }

    @Bean
    open fun createCustomerConsumerFactory(
        kafkaProperties: BroncoKafkaProperties,
    ): ConsumerFactory<String, String> {
        return createConsumerFactory(kafkaProperties.consumers, createCustomerTopic)
    }

    @Bean
    open fun createCustomerAccountConsumerFactory(
        kafkaProperties: BroncoKafkaProperties,
    ): ConsumerFactory<String, String> {
        return createConsumerFactory(kafkaProperties.consumers, createCustomerAccountTopic)
    }

    @Bean
    open fun customerListenerContainer(
        createCustomerConsumerFactory: ConsumerFactory<String, String>, kafkaProperties: BroncoKafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return createConsumerListenerContainer(
            createCustomerConsumerFactory,
            kafkaProperties.consumers,
            createCustomerTopic
        )
    }

    @Bean
    open fun customerAccountListenerContainer(
        createCustomerConsumerFactory: ConsumerFactory<String, String>, kafkaProperties: BroncoKafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return createConsumerListenerContainer(
            createCustomerConsumerFactory,
            kafkaProperties.consumers,
            createCustomerAccountTopic
        )
    }

    @KafkaLocal
    @Bean
    open fun kafkaAdmin(kafkaProperties: BroncoKafkaProperties): KafkaAdmin {
        val configMap = mapOf(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers)
        val kafkaAdmin = KafkaAdmin(configMap)
        createTopics(kafkaProperties, kafkaAdmin)
        return kafkaAdmin
    }

    @Bean
    open fun createCustomerKafkaProducerFactory(kafkaProperties: BroncoKafkaProperties): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(kafkaProperties.producers.findTopic(createCustomerTopic).producerConfig())
    }

    @Bean
    open fun createCustomerKafkaTemplate(createCustomerKafkaProducerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(createCustomerKafkaProducerFactory)
    }

    @Bean
    open fun createCustomerAccountKafkaProducerFactory(kafkaProperties: BroncoKafkaProperties): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(kafkaProperties.producers.findTopic(createCustomerAccountTopic).producerConfig())
    }

    @Bean
    open fun createCustomerAccountKafkaTemplate(createCustomerAccountKafkaProducerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(createCustomerAccountKafkaProducerFactory)
    }

    private fun createConsumerFactory(
        topics: KafkaComponentProperties,
        topicName: String
    ): ConsumerFactory<String, String> {
        val topicConfiguration = topics.findTopic(topicName)
        val kafkaConsumerConfig = topicConfiguration.consumerConfig()
        return DefaultKafkaConsumerFactory(kafkaConsumerConfig)
    }

    private fun createConsumerListenerContainer(
        consumerFactoryInstance: ConsumerFactory<String, String>, consumers: KafkaComponentProperties, topicName: String
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactoryInstance
            val topicConfig = consumers.findTopic(topicName)
            val concurrency = topicConfig.concurrency
            if (concurrency > 0) {
                setConcurrency(topicConfig.concurrency)
            }
        }
    }

    open fun createTopics(kafkaProperties: BroncoKafkaProperties, kafkaAdmin: KafkaAdmin): Unit {
        kafkaProperties.consumers.topics
            .map { NewTopic(it.topicName, 1, 1) }
            .forEach { newTopic ->
                kafkaAdmin.createOrModifyTopics(newTopic)
            }
    }
}