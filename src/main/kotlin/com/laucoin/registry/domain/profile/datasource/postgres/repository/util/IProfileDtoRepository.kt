package com.laucoin.registry.domain.profile.datasource.postgres.repository.util

import com.laucoin.registry.core.datasource.postgres.model.util.eventIdField
import com.laucoin.registry.core.datasource.postgres.model.util.idField
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.creatorJoin
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.editorJoin
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.eventJoin
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.onlyVisibleClause
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.selectCreationInformation
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.selectEditionInformation
import com.laucoin.registry.core.datasource.postgres.repository.dto.util.selectEvent
import com.laucoin.registry.domain.profile.datasource.postgres.model.ProfileDto
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileAcceptedField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileEndAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileStartAccessField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileTable
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.profileUserIdField
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.selectUser
import com.laucoin.registry.domain.profile.datasource.postgres.model.util.userJoin
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IProfileDtoRepository: ReactiveCrudRepository<ProfileDto, UUID> {
    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent, $selectUser " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin $userJoin " +
        "WHERE $onlyVisibleClause"
    )
    fun getAll(onlyVisible: Boolean): Flux<ProfileDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent, $selectUser " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin $userJoin " +
        "WHERE t.$idField = :id AND $onlyVisibleClause"
    )
    fun findById(id: UUID, onlyVisible: Boolean): Mono<ProfileDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent, $selectUser " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin $userJoin " +
        "WHERE t.$profileUserIdField = :userId " +
        "AND (:active IS FALSE OR " +
        "((t.$profileStartAccessField IS NULL OR t.$profileStartAccessField <= CURRENT_TIMESTAMP) AND " +
        "(t.$profileEndAccessField IS NULL OR t.$profileEndAccessField >= CURRENT_TIMESTAMP)))" +
        "AND t.$profileAcceptedField = :accepted " +
        "AND $onlyVisibleClause"
    )
    fun getAllByActiveAndUserId(userId: UUID, active: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<ProfileDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent, $selectUser " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin $userJoin " +
        "WHERE t.$profileUserIdField = :userId " +
        "AND (:outdated IS TRUE OR t.$profileEndAccessField IS NULL OR t.$profileEndAccessField >= CURRENT_TIMESTAMP) " +
        "AND t.$profileAcceptedField = :accepted " +
        "AND $onlyVisibleClause"
    )
    fun getAllByOutdatedAndUserId(userId: UUID, outdated: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<ProfileDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent, $selectUser " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin $userJoin " +
        "WHERE t.$eventIdField = :eventId " +
        "AND (:active IS FALSE OR " +
        "((t.$profileStartAccessField IS NULL OR t.$profileStartAccessField <= CURRENT_TIMESTAMP) AND " +
        "(t.$profileEndAccessField IS NULL OR t.$profileEndAccessField >= CURRENT_TIMESTAMP)))" +
        "AND t.$profileAcceptedField = :accepted " +
        "AND $onlyVisibleClause"
    )
    fun getAllByActiveAndEventId(eventId: UUID, active: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<ProfileDto>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent, $selectUser " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin $userJoin " +
        "WHERE t.$eventIdField = :eventId " +
        "AND (:outdated IS TRUE OR t.$profileEndAccessField IS NULL OR t.$profileEndAccessField >= CURRENT_TIMESTAMP) " +
        "AND t.$profileAcceptedField = :accepted " +
        "AND $onlyVisibleClause"
    )
    fun getAllByOutdatedAndEventId(eventId: UUID, outdated: Boolean, accepted: Boolean, onlyVisible: Boolean): Flux<ProfileDto>
}
