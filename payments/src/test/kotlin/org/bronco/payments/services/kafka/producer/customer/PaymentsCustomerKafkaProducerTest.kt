package org.bronco.payments.services.kafka.producer.customer

import com.fasterxml.jackson.databind.ObjectWriter
import org.assertj.core.api.Assertions
import org.bronco.payments.config.properties.BroncoKafkaProperties
import org.bronco.payments.initializers.KafkaInitializer
import org.bronco.payments.initializers.PostgreSqlInitializer
import org.bronco.payments.repositories.progress.ProgressRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = [PostgreSqlInitializer::class, KafkaInitializer::class])
@Import(BroncoKafkaProperties::class)
class PaymentsCustomerKafkaProducerTest {
    @Autowired
    private lateinit var kafkaProducer: PaymentsCustomerKafkaProducer
    @Autowired
    private lateinit var progressRepository: ProgressRepository
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    @Autowired
    private lateinit var objectWriter: ObjectWriter

    @Test
    fun name() {
        Assertions.assertThat(kafkaProducer).isNotNull()
        Assertions.assertThat(progressRepository).isNotNull()
        Assertions.assertThat(kafkaTemplate).isNotNull()
        Assertions.assertThat(objectWriter).isNotNull()
    }
}