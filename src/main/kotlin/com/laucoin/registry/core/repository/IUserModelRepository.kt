package com.laucoin.registry.core.repository

import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.repository.util.IGenericModelRepository
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IUserModelRepository: IGenericModelRepository<UserModel, EnrichedUserModel> {
    fun findByEmails(emails: List<String>, onlyVisible: Boolean): Flux<EnrichedUserModel>
    fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<EnrichedUserModel>
    fun findByRoles(roles: List<String>, onlyVisible: Boolean): Flux<UserModel>
}
