package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class PageDto<T>(
    var offset: Int,
    var limit: Int,
    var totalElements: Int = 0,
    var content: List<T> = emptyList(),
    val lastRefresh: ZonedDateTime = now()
) {
    constructor(offset: Int, limit: Int, pageContent: List<T>): this(offset, limit) {
        totalElements = pageContent.size
        content = pageContent.subList(
            offset.getMinWith(totalElements),
            (offset + limit).getMinWith(totalElements),
        )
    }

    companion object {
        fun <T> Flux<T>.paginate(offset: Int, limit: Int): Mono<PageDto<T>> {
            return this
                .collectList()
                .map { PageDto(offset, limit, it) }
        }
    }

    private fun Int.getMinWith(value: Int): Int {
        return if (this <= value) this else value
    }
}
