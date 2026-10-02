package fr.laucoin.registry.backend.domain.extension

import java.util.Objects

/**
 * Reflection-based null/empty checks (`isIterable`, `isNullOrEmpty`, `isNotEmpty`) that work
 * uniformly across any [Iterable] or primitive/object array without the caller knowing the concrete
 * collection type in advance.
 */
object ListExt {
	fun Any?.isIterable(): Boolean {
		return this is Iterable<*> || (Objects.nonNull(this) && this!!.javaClass.isArray)
	}

	fun Any?.isNullOrEmpty(): Boolean {
		if (Objects.isNull(this)) return true

		return when (this) {
			is Iterable<*> -> !this.iterator().hasNext()
			is Array<*> -> this.isEmpty()
			is IntArray -> this.isEmpty()
			is LongArray -> this.isEmpty()
			is DoubleArray -> this.isEmpty()
			is FloatArray -> this.isEmpty()
			is BooleanArray -> this.isEmpty()
			is CharArray -> this.isEmpty()
			is ShortArray -> this.isEmpty()
			is ByteArray -> this.isEmpty()
			else -> false
		}
	}

	fun Any?.isNotEmpty(): Boolean {
		return !isNullOrEmpty()
	}
}
