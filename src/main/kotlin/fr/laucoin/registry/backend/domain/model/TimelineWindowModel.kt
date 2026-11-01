package fr.laucoin.registry.backend.domain.model

import java.time.Duration
import java.time.ZonedDateTime

data class TimelineWindowModel(
	val start: ZonedDateTime,
	val end: ZonedDateTime,
) {
	val seconds: Long
		get() = Duration.between(start, end).seconds

	fun overlapSeconds(outing: OutingModel): Long {
		val from = if (outing.start.isAfter(start)) outing.start else start
		val to = if (outing.end.isBefore(end)) outing.end else end

		return if (to.isAfter(from)) Duration.between(from, to).seconds else 0
	}
}
