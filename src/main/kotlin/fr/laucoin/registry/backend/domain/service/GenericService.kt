package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import java.text.Normalizer.Form.NFD
import java.text.Normalizer.normalize
import java.util.Objects
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.DESC
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

open class GenericService: LoggerService() {
    fun <T: GenericModel> Flux<T>.searchAndSort(order: Direction, searched: String?, comparator: Comparator<T>? = null): Flux<T> {
        return if (searched.isNullOrBlank() && Objects.nonNull(comparator)) {
            this.sort(comparator !!.let { if (order == DESC) it.reversed() else it })
        } else {
            this
                .map { element ->
                    var counter = 0

                    (searched?.split(" ")?.map { it.removeAccent().lowercase() } ?: emptyList()).forEach {
                        element.getSearchableValues().forEach { value ->
                            if (value.removeAccent().lowercase().contains(it)) {
                                counter ++
                            }
                        }
                    }

                    Pair(element, counter)
                }
                .filter { it.second > 0 }
                .sort { p1, p2 -> p2.second.compareTo(p1.second) }
                .map { it.first }
        }
    }

    private fun String.removeAccent(): String {
        val normalized = normalize(this, NFD)
        return normalized.replace("[^\\p{ASCII}]".toRegex(), "")
    }

    fun <T: GenericModel> Mono<T>.updateVisibility(visibility: Boolean): Mono<T> {
        return this.map { it.apply { visible = visibility } }
    }
}
