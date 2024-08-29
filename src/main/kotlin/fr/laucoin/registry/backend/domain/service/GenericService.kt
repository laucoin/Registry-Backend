package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.service.impl.LoggerService
import java.text.Normalizer.Form.NFD
import java.text.Normalizer.normalize
import java.util.Objects
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.DESC
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

open class GenericService<T: GenericModel>(
    private val comparator: Comparator<T>? = null,
): LoggerService() {
    @Value("\${registry.feature.search.threshold}")
    private val searchThreshold: Double? = null

    private val textSearcher: JaroWinklerSimilarity = JaroWinklerSimilarity()

    fun Flux<T>.searchAndSort(order: Direction, searched: String?): Flux<T> {
        return if (searched.isNullOrBlank() && Objects.nonNull(comparator)) {
            this.sort(comparator !!.let { if (order == DESC) it.reversed() else it })
        } else {
            this
                .map {
                    val values: List<String> = it.getSearchableValues()
                    val similarity = values.maxOfOrNull { value ->
                        textSearcher.apply(
                            searched?.removeAccent()?.lowercase(),
                            value.removeAccent().lowercase(),
                        )
                    }
                    Pair(it, similarity)
                }
                .filter { Objects.nonNull(it.second) && it.second !! >= searchThreshold !! }
                .sort { p1, p2 -> p2.second !!.compareTo(p1.second !!) }
                .map { it.first }
        }
    }

    private fun String.removeAccent(): String {
        val normalized = normalize(this, NFD)
        return normalized.replace("[^\\p{ASCII}]".toRegex(), "")
    }

    fun Mono<T>.updateVisibility(visibility: Boolean): Mono<T> {
        return this.map { it.apply { visible = visibility } }
    }
}
