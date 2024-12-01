package fr.laucoin.registry.backend.domain.extension

import java.util.Objects
import kotlin.random.Random

object StringExt {
    fun String?.getStringBetween(delimiter: String): String? {
        if (Objects.isNull(this)) return null

        val start = this !!.indexOf(delimiter)
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
