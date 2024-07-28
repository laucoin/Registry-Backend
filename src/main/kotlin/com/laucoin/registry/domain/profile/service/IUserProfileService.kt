package com.laucoin.registry.domain.profile.service

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.domain.profile.model.EnrichedProfileModel
import com.laucoin.registry.domain.profile.model.ProfileModel
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Sort
import reactor.core.publisher.Mono

interface IUserProfileService {
    fun getPage(
        userId: UUID,
        pageIndex: Int,
        pageSize: Int,
        order: Sort.Direction,
        onlyAccepted: Boolean,
        searched: String?,
        startAccess: LocalDateTime?,
        endAccess: LocalDateTime?,
    ): Mono<PageModel<EnrichedProfileModel>>

    fun findById(currentUser: EnrichedUserModel, userId: UUID, id: UUID): Mono<EnrichedProfileModel>

    fun manageProfileAcceptance(currentUser: EnrichedUserModel, userId: UUID, id: UUID, accepted: Boolean): Mono<ProfileModel>

    fun deleteById(currentUser: EnrichedUserModel, userId: UUID, id: UUID): Mono<Void>
}
