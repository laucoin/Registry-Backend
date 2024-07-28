package com.laucoin.registry.core.datasource.postgres.repository.dto

import com.laucoin.registry.core.datasource.postgres.model.UserDto
import com.laucoin.registry.core.datasource.postgres.model.util.idField
import com.laucoin.registry.core.datasource.postgres.model.util.userEmailField
import com.laucoin.registry.core.datasource.postgres.model.util.userOidcIdField
import com.laucoin.registry.core.datasource.postgres.model.util.userRoleField
import com.laucoin.registry.core.datasource.postgres.model.util.userTable
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.creatorJoin
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.defaultProfileJoin
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.editorJoin
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.onlyVisibleClause
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.selectCreationInformation
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.selectDefaultProfile
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.selectEditionInformation
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IUserDtoRepository: ReactiveCrudRepository<UserDto, UUID> {
    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation " +
        "FROM $userTable t $creatorJoin $editorJoin " +
        "WHERE $onlyVisibleClause"
    )
    fun getAll(onlyVisible: Boolean): Flux<UserDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectDefaultProfile " +
        "FROM $userTable t $creatorJoin $editorJoin $defaultProfileJoin " +
        "WHERE t.$idField = :id AND $onlyVisibleClause"
    )
    fun findById(id: UUID, onlyVisible: Boolean): Mono<UserDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectDefaultProfile " +
        "FROM $userTable t $creatorJoin $editorJoin $defaultProfileJoin " +
        "WHERE t.$userEmailField IN (:emails) AND $onlyVisibleClause"
    )
    fun findByEmails(emails: List<String>, onlyVisible: Boolean): Flux<UserDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectDefaultProfile " +
        "FROM $userTable t $creatorJoin $editorJoin $defaultProfileJoin " +
        "WHERE t.$userRoleField IN (:roles) AND $onlyVisibleClause"
    )
    fun findByRoles(roles: List<String>, onlyVisible: Boolean): Flux<UserDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectDefaultProfile " +
        "FROM $userTable t $creatorJoin $editorJoin $defaultProfileJoin " +
        "WHERE t.$userOidcIdField = :oidcId AND $onlyVisibleClause"
    )
    fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<UserDto>
}
