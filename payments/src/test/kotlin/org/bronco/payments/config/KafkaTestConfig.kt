package org.bronco.payments.config

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
open class KafkaTestConfig {
    @Bean
    open fun adminClient(kafkaProperties: BroncoKafkaProperties): AdminClient {
        val configMap = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test.consumer.group-id"
        )
        val adminClient = AdminClient.create(configMap)
        return adminClient
    }
}