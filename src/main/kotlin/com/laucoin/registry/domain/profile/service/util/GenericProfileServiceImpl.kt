package com.laucoin.registry.domain.profile.service.util

import com.laucoin.registry.core.adapter.SecurityProperties
import com.laucoin.registry.core.service.util.GenericServiceImpl
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.repository.IProfileModelRepository
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.Objects
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

open class GenericProfileServiceImpl(
    protected val repository: IProfileModelRepository,
    securityProperties: SecurityProperties,
): GenericServiceImpl<EnrichedProfileModel>(securityProperties) {
    override fun Flux<EnrichedProfileModel>.customSort(order: Direction): Flux<EnrichedProfileModel> = sort { o1, o2 ->
        val now = now()
        compareBy<EnrichedProfileModel> { it.startAccess ?: now }
            .thenBy { it.endAccess ?: now }
            .let { comparator ->
                if (order == Direction.ASC) comparator.compare(o1, o2)
                else comparator.reversed().compare(o1, o2)
            }
    }

    protected fun getAllProfilesWithHighestRoleByEventIdExcludingUserId(
        eventId: UUID,
        userId: UUID,
    ): Mono<List<EnrichedProfileModel>> {
        val roles = securityProperties.profileRoles()
        val highestRole = roles.first()

        return repository.getAllByActiveAndEventId(
            eventId = eventId,
            active = true,
            accepted = true,
            onlyVisible = true,
        )
            .filter { it.userId != userId && Objects.nonNull(it.endAccess) && it.role == highestRole }
            .collectList()
    }

    protected fun Flux<EnrichedProfileModel>.customFilter(
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?,
    ): Flux<EnrichedProfileModel> =
        filter { listOf(it.startAccess, it.endAccess).areInRange(startAccess, endAccess) }
            .genericFilter(searched)
}
