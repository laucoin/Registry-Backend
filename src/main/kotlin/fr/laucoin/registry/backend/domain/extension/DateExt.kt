package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import java.time.LocalTime
import java.util.Objects

object DateExt {
    fun CustomDateTimeModel?.isBefore(other: CustomDateTimeModel?): Boolean {
        return when {
            Objects.isNull(other) -> false
            Objects.isNull(this) || this !!.date.isBefore(other !!.date) -> true
            date.isEqual(other.date) -> {
                val objectTime = time ?: LocalTime.MIN
                val otherTime = other.time ?: LocalTime.MIN
                objectTime !!.isBefore(otherTime)
            }

            else -> false
        }
    }

    fun CustomDateTimeModel?.isAfter(other: CustomDateTimeModel?): Boolean {
        return when {
            Objects.isNull(other) -> false
            Objects.isNull(this) || this !!.date.isAfter(other !!.date) -> true
            date.isEqual(other.date) -> {
                val objectTime = time ?: LocalTime.MIN
                val otherTime = other.time ?: LocalTime.MIN
                objectTime !!.isBefore(otherTime)
            }

            else -> false
        }
    }

    fun CustomDateTimeModel?.isBeforeOrEqual(other: CustomDateTimeModel?): Boolean {
        return when {
            Objects.isNull(this) || Objects.isNull(other) || this !!.date.isBefore(other !!.date) -> true
            date.isEqual(other.date) -> {
                val objectTime = time ?: LocalTime.MIN
                val otherTime = other.time ?: LocalTime.MIN
                objectTime !!.isBefore(otherTime) || objectTime === otherTime
            }

            else -> false
        }
    }

    fun CustomDateTimeModel?.isEqualOrAfter(other: CustomDateTimeModel?): Boolean {
        return when {
            Objects.isNull(this) || Objects.isNull(other) || this !!.date.isAfter(other !!.date) -> true
            date.isEqual(other.date) -> {
                val objectTime = time ?: LocalTime.MAX
                val otherTime = other.time ?: LocalTime.MAX
                objectTime !!.isAfter(otherTime) || objectTime === otherTime
            }

            else -> false
        }
    }

    private fun CustomDateTimeModel?.inRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
        return Objects.isNull(this) || (start.isBeforeOrEqual(this) && end.isEqualOrAfter(this))
    }

    fun CustomDateTimeModel?.notInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
        return this.inRange(start, end).not()
    }
}
