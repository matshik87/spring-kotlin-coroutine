package org.bronco.payments.config.properties

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.stereotype.Component

@Component
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "payments.kafka")
class BroncoKafkaProperties(
    var bootstrapServers: String = "",
    var consumers: KafkaConsumerProperties = KafkaConsumerProperties(),
    var producers: KafkaProducerProperties = KafkaProducerProperties()
) {
}

class KafkaConsumerProperties(
    var createTopics: Boolean = false,
    var containers: KafkaContainers = KafkaContainers(),
    var topics: KafkaTopicsProperties = KafkaTopicsProperties(),
) {}

class KafkaProducerProperties(
    var bootstrapServers: String = "",
    var createCustomer: String = "",
    var clientId: String = "",
    var keySerializer: String = "",
    var valueSerializer: String = ""
) {
    fun producerConfig(): Map<String, Any> = mapOf<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.CLIENT_ID_CONFIG to clientId,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer,
    )
}

class KafkaContainers {
    var defaultContainer: KafkaContainerDetails = KafkaContainerDetails()
}

class KafkaContainerDetails(
    var concurrency: Int = 1,
) {}

class KafkaTopicsProperties {
    var list: List<String> = emptyList()
    var createCustomer: KafkaTopicProperties = KafkaTopicProperties()
}

class KafkaTopicProperties(
    var topicName: String = "",
    var concurrency: Int = 0,
    var bootstrapServers: String = "",
    var groupId: String = "",
    var autoOffsetReset: String = "latest",
    var autoCommit: Boolean = true,
    var keyDeserializer: String = "",
    var valueDeserializer: String = ""
) {
    fun consumerConfig(): Map<String, Any> = mapOf<String, Any>(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to autoCommit,
        ConsumerConfig.GROUP_ID_CONFIG to groupId,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeserializer,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializer,
    )
}

