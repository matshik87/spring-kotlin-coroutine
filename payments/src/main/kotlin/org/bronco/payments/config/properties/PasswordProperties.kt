package org.bronco.payments.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
@EnableConfigurationProperties(PasswordProperties::class)
@ConfigurationProperties(prefix = "payments.password")
class PasswordProperties {
    var default: String = "test-password"
    var algorithm: String = "invalid-algorithm"
    var charset: Charset = Charsets.UTF_8
}