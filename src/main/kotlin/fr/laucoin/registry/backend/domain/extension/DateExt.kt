package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_RANGE_FROM_LATER_THAN_TO
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.Objects
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Objectif : Compare et valide des valeurs de date et d'heure.
 * Périmètre : Porte les règles d'ordre des date-heures personnalisées et la vérification de plage de dates.
 * Limites : Fonctions pures ; aucune E/S.
 */
object DateExt {
	fun requireOrderedRange(from: LocalDate?, to: LocalDate?) {
		requireNotReversed(from != null && to != null && from.isAfter(to))
	}

	fun requireOrderedRange(from: ZonedDateTime?, to: ZonedDateTime?) {
		requireNotReversed(from != null && to != null && from.isAfter(to))
	}

	private fun requireNotReversed(isReversed: Boolean) {
		if (isReversed) throw RegistryException(BAD_REQUEST, DATE_RANGE_FROM_LATER_THAN_TO)
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

	fun LocalDate?.isMajor(clock: Clock): Boolean {
		val today = LocalDate.now(clock)
		val minBirthday = today.minusYears(18)
		return this?.let {
			minBirthday.isAfter(it) || minBirthday.isEqual(it)
		} ?: false
	}

	fun CustomDateTimeModel?.isInRange(start: CustomDateTimeModel?, end: CustomDateTimeModel?): Boolean {
		val realStart = (start ?: CustomDateTimeModel.MIN).withTimeOrElse(OffsetTime.MIN)
		val realEnd = (end ?: CustomDateTimeModel.MAX).withTimeOrElse(OffsetTime.MAX)
		val challengerForStart = (this ?: realStart).withTimeOrElse(realStart.time)
		val challengerForEnd = (this ?: realEnd).withTimeOrElse(realEnd.time)

		val isStrictlyInRange =
			(realStart.asStartIsAfterOther(challengerForStart) || realEnd.asEndIsBeforeOther(challengerForEnd)).not()

		return isSameInstant(realStart, challengerForStart) || isSameInstant(realEnd, challengerForEnd) || isStrictlyInRange
	}

	private fun CustomDateTimeModel.withTimeOrElse(defaultTime: OffsetTime?): CustomDateTimeModel {
		return copy().also { if (it.time == null) it.time = defaultTime }
	}

	private fun isSameInstant(first: CustomDateTimeModel, second: CustomDateTimeModel): Boolean {
		return first.date.isEqual(second.date) && first.time!!.isEqual(second.time)
	}
}
