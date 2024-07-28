package com.laucoin.registry.domain.user.service

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.PageModel
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Mono

interface IUserService {
    fun getPage(
        pageIndex: Int,
        pageSize: Int,
        order: Direction,
        onlyNonBlocked: Boolean,
        searched: String?,
    ): Mono<PageModel<EnrichedUserModel>>

    fun getRoles(currentUser: EnrichedUserModel): Mono<List<String?>>
    fun findById(id: UUID): Mono<EnrichedUserModel>
    fun findByEmail(currentUser: EnrichedUserModel, searched: String): Mono<List<String?>>
    fun updateRoleById(currentUser: EnrichedUserModel, id: UUID, role: String?): Mono<UserModel>
    fun updateDefaultProfileById(currentUser: EnrichedUserModel, id: UUID, profileId: UUID): Mono<UserModel>
    fun blockById(currentUser: EnrichedUserModel, id: UUID): Mono<UserModel>
    fun unblockById(currentUser: EnrichedUserModel, id: UUID): Mono<UserModel>
    fun deleteById(currentUser: EnrichedUserModel, id: UUID): Mono<Void>
}
