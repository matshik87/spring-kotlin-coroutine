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
    var consumers: KafkaComponentProperties = KafkaComponentProperties(),
    var producers: KafkaComponentProperties = KafkaComponentProperties()
) {
}

class KafkaComponentProperties(
    var topics: List<KafkaTopicProperties> = emptyList(),
) {
    fun findTopic(topicName: String): KafkaTopicProperties = topics.find { it.name == topicName }
        ?: throw IllegalArgumentException("Missing $topicName topic configuration. Could not proceed.")
}

class KafkaTopicProperties(
    var name: String = "",
    var topicName: String = "",
    var concurrency: Int = 0,
    var bootstrapServers: String = "",
    var groupId: String = "",
    var clientId: String = "",
    var autoOffsetReset: String = "latest",
    var autoCommit: Boolean = true,
    var serialization: SerializationConfig = SerializationConfig(),
    var deserialization: DeserializationConfig = DeserializationConfig(),
) {
    fun consumerConfig(): Map<String, Any> = mapOf<String, Any>(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to autoCommit,
        ConsumerConfig.GROUP_ID_CONFIG to groupId.ifBlank { "$topicName-groupId" },
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to deserialization.key,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to deserialization.value,
    )
    fun producerConfig(): Map<String, Any> = mapOf<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.CLIENT_ID_CONFIG to clientId.ifBlank { "$topicName-client" },
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to serialization.key,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to serialization.value,
    )
}

class SerializationConfig(
    var key: String = "",
    var value: String = ""
)

class DeserializationConfig(
    var key: String = "",
    var value: String = ""
)
