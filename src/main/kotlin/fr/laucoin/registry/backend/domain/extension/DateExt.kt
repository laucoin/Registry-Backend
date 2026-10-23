package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_RANGE_FROM_LATER_THAN_TO
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.Objects
import org.springframework.http.HttpStatus.BAD_REQUEST

object DateExt {
	fun requireOrderedRange(from: LocalDate?, to: LocalDate?) {
		if (from != null && to != null && from.isAfter(to)) {
			throw RegistryException(BAD_REQUEST, DATE_RANGE_FROM_LATER_THAN_TO)
		}
	}

	fun requireOrderedRange(from: ZonedDateTime?, to: ZonedDateTime?) {
		if (from != null && to != null && from.isAfter(to)) {
			throw RegistryException(BAD_REQUEST, DATE_RANGE_FROM_LATER_THAN_TO)
		}
	}

	fun CustomDateTimeModel?.asStartIsAfterOther(other: CustomDateTimeModel?): Boolean {
		return when {
			Objects.isNull(other) -> Objects.nonNull(this)
			Objects.isNull(this) -> false
			this!!.date.isAfter(other!!.date) -> true
			this.date.isEqual(other.date) -> this.time.asStartIsAfterOther(other.time)

			else -> false
		}
	}

	private fun OffsetTime?.asStartIsAfterOther(other: OffsetTime?): Boolean {
		return when {
			Objects.isNull(other) -> Objects.nonNull(this)
			Objects.isNull(this) -> false
			else -> this!!.isAfter(other)
		}
	}

	fun CustomDateTimeModel?.asEndIsBeforeOther(other: CustomDateTimeModel?): Boolean {
		return when {
			Objects.isNull(other) -> Objects.nonNull(this)
			Objects.isNull(this) -> false
			this!!.date.isBefore(other!!.date) -> true
			this.date.isEqual(other.date) -> this.time.asEndIsBeforeOther(other.time)

			else -> false
		}
	}

	private fun OffsetTime?.asEndIsBeforeOther(other: OffsetTime?): Boolean {
		return when {
			Objects.isNull(other) -> Objects.nonNull(this)
			Objects.isNull(this) -> false
			else -> this!!.isBefore(other)
		}
	}

	fun LocalDate?.isMajor(): Boolean {
		val today = LocalDate.now()
		val minBirthday = today.minusYears(18)
		return this?.let {
			minBirthday.isAfter(it) || minBirthday.isEqual(it)
		} ?: false
	}

	fun CustomDateTimeModel?.isInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
		val realStart = (start ?: CustomDateTimeModel.MIN)
		if (Objects.isNull(realStart.time)) realStart.time = OffsetTime.MIN
		val realEnd = (end ?: CustomDateTimeModel.MAX)
		if (Objects.isNull(realEnd.time)) realEnd.time = OffsetTime.MAX
		val realChallengerForStart = this ?: realStart
		if (Objects.isNull(realChallengerForStart.time)) realChallengerForStart.time = realStart.time
		val realChallengerForEnd = this ?: realEnd
		if (Objects.isNull(realChallengerForEnd.time)) realChallengerForEnd.time = realEnd.time

		val isEqualStart =
			realStart.date.isEqual(realChallengerForStart.date) && realStart.time!!.isEqual(realChallengerForStart.time)
		val isEqualEnd =
			realEnd.date.isEqual(realChallengerForEnd.date) && realEnd.time!!.isEqual(realChallengerForEnd.time)
		val isStrictlyInRange =
			(realStart.asStartIsAfterOther(realChallengerForStart) || realEnd.asEndIsBeforeOther(realChallengerForEnd)).not()

		return isEqualStart || isEqualEnd || isStrictlyInRange
	}
}
