package com.laucoin.registry.core.repository

import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.model.user.userTable
import com.laucoin.registry.core.repository.util.IGenericRepository
import com.laucoin.registry.core.repository.util.creatorJoin
import com.laucoin.registry.core.repository.util.defaultProfileJoin
import com.laucoin.registry.core.repository.util.editorJoin
import com.laucoin.registry.core.repository.util.onlyVisibleClause
import com.laucoin.registry.core.repository.util.selectCreationInformation
import com.laucoin.registry.core.repository.util.selectDefaultProfile
import com.laucoin.registry.core.repository.util.selectEditionInformation
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IUserRepository: IGenericRepository<UserModel, UUID> {
    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation " +
        "FROM $userTable t $creatorJoin $editorJoin " +
        "WHERE $onlyVisibleClause"
    )
    override fun getAll(onlyVisible: Boolean): Flux<UserModel>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectDefaultProfile " +
        "FROM $userTable t $creatorJoin $editorJoin $defaultProfileJoin " +
        "WHERE t.id = :id AND $onlyVisibleClause"
    )
    override fun findById(id: UUID, onlyVisible: Boolean): Mono<UserModel>

    @Query(
        "SELECT t.*, $selectCreationInformation, $selectEditionInformation, $selectDefaultProfile " +
        "FROM $userTable t $creatorJoin $editorJoin $defaultProfileJoin " +
        "WHERE t.oidc_id = :oidcId AND $onlyVisibleClause"
    )
    fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<UserModel>
}
