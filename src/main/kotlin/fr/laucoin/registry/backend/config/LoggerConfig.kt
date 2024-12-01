package fr.laucoin.registry.backend.config

import ch.qos.logback.classic.Level.DEBUG
import ch.qos.logback.classic.Level.ERROR
import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Level.WARN
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants.DEFAULT_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.GREEN_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.MAGENTA_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.RED_FG
import ch.qos.logback.core.pattern.color.ANSIConstants.YELLOW_FG
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

class LoggerConfig: ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent): String {
        return when (event.level) {
            ERROR -> RED_FG
            DEBUG -> GREEN_FG
            INFO -> MAGENTA_FG
            WARN -> YELLOW_FG
            else -> DEFAULT_FG
        }
    }
}
