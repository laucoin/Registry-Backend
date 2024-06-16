package com.laucoin.registry.core.datasource.postgres.repository

import com.laucoin.registry.core.datasource.postgres.model.UserDto
import com.laucoin.registry.core.datasource.postgres.repository.dto.IUserDtoRepository
import com.laucoin.registry.core.model.user.EnrichedUserModel
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.repository.IUserModelRepository
import com.laucoin.registry.core.util.Logger
import com.laucoin.registry.core.util.notFoundIfEmpty
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserModelRepository(
    private val repository: IUserDtoRepository
): IUserModelRepository, Logger() {
    override fun findByEmails(emails: List<String>, onlyVisible: Boolean): Flux<EnrichedUserModel> {
        log.debug("Finding onlyVisible \"{}\" users by emails \"{}\"", onlyVisible, emails)
        return repository.findByEmails(emails, onlyVisible)
            .map { it.toModel() }
    }

    override fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<EnrichedUserModel> {
        log.debug("Finding onlyVisible \"{}\" user by OIDC ID \"{}\"", onlyVisible, oidcId)
        return repository.findByOidcId(oidcId, onlyVisible)
            .map { it.toModel() }
    }

    override fun findByRoles(roles: List<String>, onlyVisible: Boolean): Flux<UserModel> {
        log.debug("Finding onlyVisible \"{}\" users by roles \"{}\"", onlyVisible, roles)
        return repository.findByRoles(roles, onlyVisible)
            .map { it.toModel() }
    }

    override fun getAll(onlyVisible: Boolean): Flux<EnrichedUserModel> {
        log.debug("Getting all users onlyVisible \"{}\"", onlyVisible)
        return repository.getAll(onlyVisible)
            .map { it.toModel() }
    }

    override fun findById(id: UUID, onlyVisible: Boolean): Mono<EnrichedUserModel> {
        log.debug("Finding onlyVisible \"{}\" user by ID \"{}\"", onlyVisible, id)
        return repository.findById(id, onlyVisible)
            .notFoundIfEmpty(id)
            .map { it.toModel() }
    }

    override fun create(element: UserModel): Mono<UserModel> {
        log.debug("Creating user \"{}\"", element)
        return repository.save(UserDto(element))
            .map { it.toModel() }
    }

    override fun updateById(id: UUID, element: UserModel): Mono<UserModel> {
        element.id = id
        log.debug("Updating user by ID \"{}\" with values \"{}\"", id, element)
        return repository.save(UserDto(element))
            .map { it.toModel() }
    }

    override fun deleteById(id: UUID): Mono<Void> {
        log.debug("Deleting user by ID \"{}\"", id)
        return repository.deleteById(id)
    }
}
