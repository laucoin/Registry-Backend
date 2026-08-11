package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.Objects

object DateExt {
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

	/**
	 * Containment on the INSTANT line. Every value is resolved through its own
	 * boundary ([CustomDateTimeModel.asStart] / [CustomDateTimeModel.asEnd]), so
	 * a bare date is the WHOLE DAY it names — midnight to 23:59:59 — while a
	 * value carrying a time is the single instant it states. The window opens at
	 * its start's midnight and closes at its end's 23:59:59, which is what lets
	 * one date serve as both ends of a one-day window.
	 *
	 * The challenger is in range when the day (or instant) it denotes OVERLAPS
	 * that window, not when it is swallowed whole: this overload is asked about a
	 * single value without being told whether it is an arrival or a departure, so
	 * demanding containment would reject a bare date sitting on the window's last
	 * day the moment that day had a closing time — the participant arriving the
	 * morning the project ends at 19:43. Callers that DO know which end they hold
	 * use [isStartInRange] / [isEndInRange], which are exact.
	 *
	 * Nothing is mutated, here or on the caller's models: an earlier version
	 * assigned `OffsetTime.MIN`/`MAX` onto copies, and those ±18 h offsets are
	 * outside what Postgres accepts — one leak turned a null availability time
	 * into `00:00:00+18` on insert (BadSqlGrammar).
	 */
	fun CustomDateTimeModel?.isInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
		val from = (start ?: CustomDateTimeModel.MIN).asStart()
		val until = (end ?: CustomDateTimeModel.MAX).asEnd()
		val challengerStart = this?.asStart() ?: from
		val challengerEnd = this?.asEnd() ?: until

		return !challengerStart.isAfter(until) && !challengerEnd.isBefore(from)
	}

	/**
	 * The challenger is an ARRIVAL, so a bare date is the midnight that opens it:
	 * that exact instant must fall inside the window.
	 */
	fun CustomDateTimeModel?.isStartInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
		return this?.asStart().isInstantInRange(start, end)
	}

	/**
	 * The challenger is a DEPARTURE, so a bare date is the 23:59:59 that closes
	 * it. Pairing this with [isStartInRange] is what lets an arrival and a
	 * departure share one date and still both sit inside a one-day window.
	 */
	fun CustomDateTimeModel?.isEndInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
		return this?.asEnd().isInstantInRange(start, end)
	}

	private fun ZonedDateTime?.isInstantInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
		if (this == null) {
			return true
		}
		val from = (start ?: CustomDateTimeModel.MIN).asStart()
		val until = (end ?: CustomDateTimeModel.MAX).asEnd()
		return !isBefore(from) && !isAfter(until)
	}
}
