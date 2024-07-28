package com.laucoin.registry.core.util

import com.laucoin.registry.core.model.util.ErrorEnum.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

private fun <T> Mono<List<T>>.paginate(pageIndex: Int, pageSize: Int): Mono<PageModel<T>> {
    return this
        .map {
            PageModel(pageIndex, pageSize, it)
        }
}

fun <T> Flux<T>.paginate(pageIndex: Int, pageSize: Int): Mono<PageModel<T>> {
    return this
        .collectList()
        .paginate(pageIndex, pageSize)
}

fun <T> Mono<T>.notFoundIfEmpty(identifier: Any): Mono<T> {
    return this.switchIfEmpty {
        throw RegistryExceptionModel(
            status = NOT_FOUND,
            errorCode = NOT_FOUND_WITH_GIVEN_IDENTIFIER.name,
            args = listOf(identifier),
        )
    }
}
