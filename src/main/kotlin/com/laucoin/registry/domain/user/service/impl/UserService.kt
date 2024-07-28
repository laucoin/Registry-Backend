package com.laucoin.registry.domain.user.service.impl

import com.laucoin.registry.core.adapter.SecurityProperties
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.util.ErrorEnum.CANT_ALTER_LAST_USER_MANAGER
import com.laucoin.registry.core.model.util.ErrorEnum.CANT_ALTER_SERVICE_ACCOUNT
import com.laucoin.registry.core.model.util.ErrorEnum.CANT_BLOCK_YOURSELF
import com.laucoin.registry.core.model.util.ErrorEnum.NOT_SELECTABLE_PROFILE
import com.laucoin.registry.core.model.util.PageModel
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import com.laucoin.registry.core.repository.IUserModelRepository
import com.laucoin.registry.core.service.util.GenericServiceImpl
import com.laucoin.registry.core.util.notFoundIfEmpty
import com.laucoin.registry.core.util.paginate
import com.laucoin.registry.domain.profile.repository.IProfileModelRepository
import com.laucoin.registry.domain.user.service.IUserService
import java.time.LocalDateTime.now
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(
    @Value("\${registry.feature.user.search.email.max-results:20}")
    private val maxResults: Long,
    private val repository: IUserModelRepository,
    private val profileRepository: IProfileModelRepository,
    securityProperties: SecurityProperties,
): IUserService, GenericServiceImpl<EnrichedUserModel>(securityProperties) {
    override fun getPage(
        pageIndex: Int,
        pageSize: Int,
        order: Direction,
        onlyNonBlocked: Boolean,
        searched: String?,
    ): Mono<PageModel<EnrichedUserModel>> =
        repository.getAll(onlyNonBlocked)
            .genericFilter(searched)
            .customSort(order)
            .fillServiceAccountUser()
            .paginate(pageIndex, pageSize)

    override fun getRoles(currentUser: EnrichedUserModel): Mono<List<String?>> =
        Mono.just(securityProperties.userRoles())

    override fun findById(id: UUID): Mono<EnrichedUserModel> =
        repository.findById(id, onlyVisible = false)
            .fillServiceAccountUser()

    override fun findByEmail(currentUser: EnrichedUserModel, searched: String): Mono<List<String?>> =
        repository.getAll(onlyVisible = true)
            .filter { searched.isOneMatch(listOf(it.email)) }
            .sort { o1, o2 ->
                compareBy<EnrichedUserModel> { it.email ?: "" }.compare(o1, o2)
            }
            .mapNotNull(EnrichedUserModel::email)
            .take(maxResults)
            .collectList()

    override fun updateRoleById(currentUser: EnrichedUserModel, id: UUID, role: String?): Mono<UserModel> =
        alterationCheck(id)
            .flatMap {
                it.role = role
                it.update(currentUser)
                repository.updateById(id, it)
            }

    override fun updateDefaultProfileById(currentUser: EnrichedUserModel, id: UUID, profileId: UUID): Mono<UserModel> {
        return profileRepository.findById(profileId, onlyVisible = true)
            .filter { it.userId == id }
            .notFoundIfEmpty(profileId)
            .handle { it, handle ->
                val now = now()
                if (
                    (Objects.nonNull(it.startAccess) && it.startAccess !!.isAfter(now))
                    || (Objects.nonNull(it.endAccess) || it.endAccess !!.isBefore(now))
                ) {

                    log.warn("User \"{}\" attempt to select profile \"{}\" as default", id, profileId)
                    handle.error(
                        RegistryExceptionModel(
                            status = FORBIDDEN,
                            errorCode = NOT_SELECTABLE_PROFILE.name
                        )
                    )
                }

                handle.next(it)
            }
            .flatMap { repository.findById(id, onlyVisible = true) }
            .flatMap {
                it.defaultProfileId = profileId
                it.update(currentUser)
                repository.updateById(id, it)
            }
    }

    override fun blockById(currentUser: EnrichedUserModel, id: UUID): Mono<UserModel> {
        if (currentUser.id == id) {
            return Mono.error(
                RegistryExceptionModel(
                    status = FORBIDDEN,
                    errorCode = CANT_BLOCK_YOURSELF.name
                )
            )
        }

        return alterationCheck(id)
            .flatMap {
                it.visible = false
                it.update(currentUser)
                repository.updateById(id, it)
            }
    }

    override fun unblockById(currentUser: EnrichedUserModel, id: UUID): Mono<UserModel> =
        repository.findById(id, onlyVisible = false)
            .filter { ! it.visible }
            .flatMap {
                it.visible = true
                it.update(currentUser)
                repository.updateById(id, it)
            }

    override fun deleteById(currentUser: EnrichedUserModel, id: UUID): Mono<Void> =
        alterationCheck(id)
            .flatMap { repository.deleteById(id) }

    private fun alterationCheck(id: UUID): Mono<EnrichedUserModel> =
        Mono.just(id)
            .serviceAccountCheck()
            .flatMap { repository.findById(it, onlyVisible = true) }
            .lastUserManagerCheck()

    private fun Mono<UUID>.serviceAccountCheck(): Mono<UUID> {
        return this.handle { it, handle ->
            if (it == serviceAccount.id) {
                log.warn("Service account \"{}\" alteration attempt blocked", it)
                handle.error(
                    RegistryExceptionModel(
                        status = FORBIDDEN,
                        errorCode = CANT_ALTER_SERVICE_ACCOUNT.name
                    )
                )
            } else {
                handle.next(it)
            }
        }
    }

    private fun Mono<EnrichedUserModel>.lastUserManagerCheck(): Mono<EnrichedUserModel> =
        this.flatMap { user ->
            val userManagementRoles = securityProperties.userManagerRoles()
            repository.findByRoles(userManagementRoles, onlyVisible = true)
                .filter { it.id != user.id }
                .collectList()
                .handle { it, handle ->
                    if (it.isEmpty()) {
                        log.warn("Last user manager \"{}\" alteration attempt blocked", user.id)
                        handle.error(
                            RegistryExceptionModel(
                                status = FORBIDDEN,
                                errorCode = CANT_ALTER_LAST_USER_MANAGER.name
                            )
                        )
                    } else {
                        handle.next(it)
                    }
                }
                .map { user }
        }

    override fun fillServiceAccountUser(element: EnrichedUserModel): EnrichedUserModel {
        if (element.id == serviceAccount.id) return serviceAccount
        element.fillHistoryWithServiceAccountIfNecessary(serviceAccount)
        return element
    }

    override fun Flux<EnrichedUserModel>.customSort(order: Direction): Flux<EnrichedUserModel> = sort { o1, o2 ->
        compareBy<EnrichedUserModel> { it.lastName ?: "" }
            .thenBy { it.firstName ?: "" }
            .let { comparator ->
                if (order == Direction.ASC) comparator.compare(o1, o2)
                else comparator.reversed().compare(o1, o2)
            }
    }
}
