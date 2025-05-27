package org.bronco.payments.config

import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@EnableKafka
@Configuration
open class KafkaConfiguration {

    @Bean
    open fun createCustomerConsumerFactory(
        kafkaProperties: BroncoKafkaProperties,
        broncoKafkaProperties: BroncoKafkaProperties
    ): ConsumerFactory<String, String> {
        val createCustomerTopic = broncoKafkaProperties.consumers.topics.createCustomer
        val kafkaConsumerConfig = createCustomerTopic.consumerConfig()
        return DefaultKafkaConsumerFactory(kafkaConsumerConfig)
    }

    @Bean
    open fun createCustomerListenerContainer(
        createCustomerConsumerFactory: ConsumerFactory<String, String>, kafkaProperties: BroncoKafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val kafkaConsumerFactory = ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = createCustomerConsumerFactory
            setConcurrency(kafkaProperties.consumers.topics.createCustomer.concurrency)
        }

        return kafkaConsumerFactory
    }

}