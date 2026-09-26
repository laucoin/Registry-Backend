package fr.laucoin.registry.backend.domain.extension

import java.util.Objects
import kotlin.random.Random

/**
 * Small String helpers: [getStringBetween] extracts the substring between two occurrences of a
 * delimiter, and [generateRandomString] produces a short random alphanumeric token.
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

	private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
	fun generateRandomString(): String {
		val length = 10
		val stringBuilder = StringBuilder(length)

		for (i in 0 until length) {
			val randomIndex = Random.nextInt(CHARACTERS.length)
			stringBuilder.append(CHARACTERS[randomIndex])
		}

		return stringBuilder.toString()
	}
}
