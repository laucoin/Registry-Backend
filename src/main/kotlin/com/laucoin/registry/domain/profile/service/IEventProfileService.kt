package com.laucoin.registry.domain.profile.service

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import com.laucoin.registry.domain.profile.model.ProfilesCreationModel
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventProfileService {
    fun getPage(
        eventId: UUID,
        pageIndex: Int,
        pageSize: Int,
        order: Direction,
        onlyNonBlocked: Boolean,
        onlyAccepted: Boolean,
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?,
    ): Mono<PageModel<EnrichedProfileModel>>

    fun findById(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        id: UUID,
    ): Mono<EnrichedProfileModel>

    fun getRoles(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        id: UUID?,
    ): Mono<List<String?>>

    fun createSupportProfile(
        currentUser: EnrichedUserModel,
        role: String,
        eventId: UUID,
    ): Mono<ProfileModel>

    fun createMultiple(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        profiles: ProfilesCreationModel,
    ): Flux<ProfileModel>

    fun updateById(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        id: UUID,
        profile: ProfileModel,
    ): Mono<ProfileModel>

    fun blockById(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        id: UUID,
    ): Mono<ProfileModel>

    fun unblockById(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        id: UUID,
    ): Mono<ProfileModel>

    fun deleteById(
        currentUser: EnrichedUserModel,
        eventId: UUID,
        id: UUID,
    ): Mono<Void>
}
