package org.bronco.payments.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ObjectMapperConfig {
    @Bean
    open fun objectMapper(): ObjectMapper = jacksonObjectMapper()

    @Bean
    open fun objectWriter(objectMapper: ObjectMapper): ObjectWriter = objectMapper.writer()

    @Bean
    open fun objectReader(objectMapper: ObjectMapper): ObjectReader = objectMapper.reader()
}