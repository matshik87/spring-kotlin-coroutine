package org.bronco.payments.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.bronco.payments.config.conditionals.KafkaLocal
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaAdmin

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
    open fun customerListenerContainer(
        createCustomerConsumerFactory: ConsumerFactory<String, String>, kafkaProperties: BroncoKafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val kafkaConsumerFactory = ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = createCustomerConsumerFactory
            setConcurrency(kafkaProperties.consumers.topics.createCustomer.concurrency)
        }

        return kafkaConsumerFactory
    }

    @KafkaLocal
    @Bean
    open fun kafkaAdmin(kafkaProperties: BroncoKafkaProperties): KafkaAdmin {
        val configMap = mapOf(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers)
        val kafkaAdmin = KafkaAdmin(configMap)
        createTopics(kafkaProperties, kafkaAdmin)
        return kafkaAdmin
    }

    open fun createTopics(kafkaProperties: BroncoKafkaProperties, kafkaAdmin: KafkaAdmin): Unit {
        kafkaProperties.consumers.topics.list
            .map { topicName -> NewTopic(topicName, 1, 1) }
            .forEach { newTopic ->
                kafkaAdmin.createOrModifyTopics(newTopic)
            }
    }
}