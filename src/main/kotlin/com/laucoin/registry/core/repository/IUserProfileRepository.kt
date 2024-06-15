package com.laucoin.registry.core.repository

import com.laucoin.registry.core.model.profile.UserProfileModel
import com.laucoin.registry.core.model.profile.profileTable
import com.laucoin.registry.core.repository.util.IGenericRepository
import com.laucoin.registry.core.repository.util.creatorJoin
import com.laucoin.registry.core.repository.util.editorJoin
import com.laucoin.registry.core.repository.util.eventJoin
import com.laucoin.registry.core.repository.util.onlyVisibleClause
import com.laucoin.registry.core.repository.util.selectCreationInformation
import com.laucoin.registry.core.repository.util.selectEditionInformation
import com.laucoin.registry.core.repository.util.selectEvent
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IUserProfileRepository: IGenericRepository<UserProfileModel, UUID> {
    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin " +
        "WHERE $onlyVisibleClause"
    )
    override fun getAll(onlyVisible: Boolean): Flux<UserProfileModel>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectEvent " +
        "FROM $profileTable t $creatorJoin $editorJoin $eventJoin " +
        "WHERE t.id = :id AND $onlyVisibleClause"
    )
    override fun findById(id: UUID, onlyVisible: Boolean): Mono<UserProfileModel>
}
