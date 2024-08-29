package fr.laucoin.registry.backend.domain.extension

import java.time.ZonedDateTime
import java.util.Objects.isNull

object DateExt {
    fun ZonedDateTime?.inRange(start: ZonedDateTime?, end: ZonedDateTime?): Boolean {
        return isNull(this) ||
               (
                       (isNull(start) || ! this !!.isBefore(start))
                       && (isNull(end) || ! this !!.isAfter(end))
               )
    }

    fun ZonedDateTime?.notInRange(start: ZonedDateTime?, end: ZonedDateTime?): Boolean {
        return this.inRange(start, end).not()
    }
}
