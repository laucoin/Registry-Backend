package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_MEMBERS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import java.time.ZonedDateTime
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(GROUP_TABLE)
data class GroupEntity(
    @Column(GROUP_NAME)
    var name: String? = null,
    @Column(GROUP_BEGIN)
    var begin: ZonedDateTime? = null,
    @Column(GROUP_END)
    var end: ZonedDateTime? = null,
    @ReadOnlyProperty
    @Column(GROUP_MEMBERS)
    var members: String = "[]",
): GenericEventEntity()
