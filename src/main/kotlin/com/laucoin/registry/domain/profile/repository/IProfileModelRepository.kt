package com.laucoin.registry.domain.profile.repository

import com.laucoin.registry.core.repository.util.IGenericModelRepository
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import java.util.UUID
import reactor.core.publisher.Flux

interface IProfileModelRepository: IGenericModelRepository<ProfileModel, EnrichedProfileModel> {
    fun getAllByActiveAndUserId(userId: UUID, active: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<EnrichedProfileModel>
    fun getAllByOutdatedAndUserId(userId: UUID, outdated: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<EnrichedProfileModel>
    fun getAllByActiveAndEventId(eventId: UUID, active: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<EnrichedProfileModel>
    fun getAllByOutdatedAndEventId(
        eventId: UUID,
        outdated: Boolean,
        accepted: Boolean,
        onlyVisible: Boolean
    ): Flux<EnrichedProfileModel>

    fun createAll(elements: Iterable<ProfileModel>): Flux<ProfileModel>
}
