package org.bronco.payments.utils

import java.util.UUID

fun String.toUuid(): UUID = UUID.fromString(this)
