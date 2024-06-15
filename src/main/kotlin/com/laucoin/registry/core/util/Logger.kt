package com.laucoin.registry.core.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class Logger {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
}
