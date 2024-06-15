package com.laucoin.registry.core.repository

import com.laucoin.registry.core.model.event.eventTable
import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.repository.util.IGenericRepository
import com.laucoin.registry.core.repository.util.addressJoin
import com.laucoin.registry.core.repository.util.creatorJoin
import com.laucoin.registry.core.repository.util.editorJoin
import com.laucoin.registry.core.repository.util.onlyVisibleClause
import com.laucoin.registry.core.repository.util.selectAddress
import com.laucoin.registry.core.repository.util.selectCreationInformation
import com.laucoin.registry.core.repository.util.selectEditionInformation
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IEventRepository: IGenericRepository<UserModel, UUID> {
    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectAddress " +
        "FROM $eventTable t $creatorJoin $editorJoin $addressJoin " +
        "WHERE $onlyVisibleClause"
    )
    override fun getAll(onlyVisible: Boolean): Flux<UserModel>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectAddress " +
        "FROM $eventTable t $creatorJoin $editorJoin $addressJoin " +
        "WHERE t.id = :id AND $onlyVisibleClause"
    )
    override fun findById(id: UUID, onlyVisible: Boolean): Mono<UserModel>
}
