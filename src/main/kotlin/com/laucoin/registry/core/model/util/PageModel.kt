package com.laucoin.registry.core.model.util

import java.time.LocalDateTime
import java.time.LocalDateTime.now

data class PageModel<T>(
    var index: Int,
    var size: Int,
    var totalElements: Int = 0,
    var content: List<T> = emptyList(),
    val lastRefresh: LocalDateTime = now()
) {
    constructor(pageIndex: Int, pageSize: Int, pageContent: List<T>): this(pageIndex, pageSize) {
        index = pageIndex
        size = pageSize
        totalElements = pageContent.size

        val from = index * size
        content = pageContent.subList(
            from.getMinWith(totalElements),
            (from + size).getMinWith(totalElements),
        )
    }

    private fun Int.getMinWith(value: Int): Int {
        return if (this <= value) this else value
    }
}
