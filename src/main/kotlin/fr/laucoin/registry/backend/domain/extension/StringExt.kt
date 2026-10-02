package fr.laucoin.registry.backend.domain.extension

import java.util.Objects

/**
 * Small String helper: [getStringBetween] extracts the substring between two occurrences of a
 * delimiter.
 */
object StringExt {
	fun String?.getStringBetween(delimiter: String): String? {
		if (Objects.isNull(this)) return null

		val start = this!!.indexOf(delimiter)
		val end = lastIndexOf(delimiter)

		val startIndex = start + 1
		if (start == end) return null
		return subSequence(startIndex, end).toString()
	}
}
