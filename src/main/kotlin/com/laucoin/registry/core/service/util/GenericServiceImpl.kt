package com.laucoin.registry.core.service.util

import com.laucoin.registry.core.adapter.SecurityProperties
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.util.ErrorEnum.START_CANT_BE_LATER_THAN_END
import com.laucoin.registry.core.model.util.GenericModel
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.util.Logger
import java.text.Normalizer.Form.NFD
import java.text.Normalizer.normalize
import java.time.LocalDateTime
import java.util.Objects
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.BAD_REQUEST
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

abstract class GenericServiceImpl<T: GenericModel>(
    protected val securityProperties: SecurityProperties,
): Logger() {
    private val defaultThreshold = 0.80F
    protected val serviceAccount: EnrichedUserModel = securityProperties.serviceAccount()

    protected abstract fun Flux<T>.customSort(order: Direction): Flux<T>

    protected open fun Flux<T>.genericFilter(searched: String?, threshold: Float = defaultThreshold): Flux<T> =
        filter { it.filterFields().isSearchMatch(searched, threshold) }

    private fun List<String?>.isSearchMatch(searched: String?, threshold: Float): Boolean {
        if (searched.isNullOrBlank()) return true

        val searchedStrings = searched.split(" ")

        val results = searchedStrings.map { it.isOneMatch(this, threshold) }

        return ! results.contains(false)
    }

    private fun String.removeAccentAndLowerCase(): String {
        val normalized = normalize(this, NFD)
        val withoutAccents = normalized.replace("[^\\p{ASCII}]".toRegex(), "")
        return withoutAccents.lowercase()
    }

    private fun String.isMatchWithJaroWinkler(value: String, threshold: Float = defaultThreshold): Boolean {
        val percentageOfMatching = JaroWinklerSimilarity().apply(value, this)
        return percentageOfMatching >= threshold
    }

    private fun String.isMatchWithCustomSearch(value: String): Boolean {
        return this.contains(value) || value.contains(this)
    }

    protected fun String.isOneMatch(values: List<String?>, threshold: Float = defaultThreshold): Boolean {
        val iterator = values.iterator()
        var isMatch = false

        while (! isMatch && iterator.hasNext()) {
            val value = iterator.next()
            if (value.isNullOrBlank()) continue

            val searchedFormatted = this.removeAccentAndLowerCase()
            val valueFormatted = value.removeAccentAndLowerCase()

            val isMatchWithJaroWinkler = searchedFormatted.isMatchWithJaroWinkler(valueFormatted, threshold)
            val isMatchWithCustomSearch = searchedFormatted.isMatchWithCustomSearch(valueFormatted)
            isMatch = isMatchWithJaroWinkler || isMatchWithCustomSearch
        }

        return isMatch
    }

    private fun LocalDateTime.isInRange(start: LocalDateTime?, end: LocalDateTime?): Boolean {
        if (Objects.nonNull(start) && Objects.nonNull(end) && end !!.isBefore(start)) {
            throw RegistryExceptionModel(
                status = BAD_REQUEST,
                errorCode = START_CANT_BE_LATER_THAN_END.name,
                args = listOf(start !!, end),
            )
        }

        return (Objects.isNull(start) || start !!.isBefore(this) || start.isEqual(this))
               && (Objects.isNull(end) || end !!.isAfter(this) || end.isEqual(this))
    }

    protected fun List<LocalDateTime?>.areInRange(start: LocalDateTime?, end: LocalDateTime?): Boolean {
        var areInRange = false
        val iterator = this.iterator()
        while (! areInRange && iterator.hasNext()) {
            val value = iterator.next()
            areInRange = if (Objects.nonNull(value)) {
                value !!.isInRange(start, end)
            } else true
        }
        return areInRange
    }

    protected open fun fillServiceAccountUser(element: T): T {
        element.fillHistoryWithServiceAccountIfNecessary(serviceAccount)
        return element
    }

    protected fun Mono<T>.fillServiceAccountUser(): Mono<T> = this.map { fillServiceAccountUser(it) }
    protected fun Flux<T>.fillServiceAccountUser(): Flux<T> = this.map { fillServiceAccountUser(it) }
}
