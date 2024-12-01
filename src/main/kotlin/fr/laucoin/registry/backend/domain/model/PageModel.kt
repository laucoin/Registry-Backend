package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class PageModel<T: GenericModel>(
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
        fun <T: GenericModel> Flux<T>.paginate(offset: Int, limit: Int): Mono<PageModel<T>> {
            return this
                .collectList()
                .map { PageModel(offset, limit, it) }
        }
    }

    private fun Int.getMinWith(value: Int): Int {
        return if (this <= value) this else value
    }
}
