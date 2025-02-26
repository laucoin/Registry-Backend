package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATED_AT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.CREATOR_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LAST_MODIFIER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.VISIBLE
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

abstract class GenericEntity(
    @Id
    @Column(ID)
    var id: UUID? = null,
    @Column(VISIBLE)
    var visible: Boolean? = null,

    @Column(CREATED_AT)
    var createdAt: ZonedDateTime? = null,
    @Column(CREATOR_ID)
    var creatorId: UUID? = null,
    @ReadOnlyProperty
    @Column(CREATOR_FIRST_NAME)
    var creatorFirstName: String? = null,
    @ReadOnlyProperty
    @Column(CREATOR_LAST_NAME)
    var creatorLastName: String? = null,
    @ReadOnlyProperty
    @Column(CREATOR_EMAIL)
    var creatorEmail: String? = null,

    @Column(LAST_MODIFIER_DATE)
    var lastUpdateAt: ZonedDateTime? = null,
    @Column(LAST_MODIFIER_ID)
    var lastEditorId: UUID? = null,
    @ReadOnlyProperty
    @Column(LAST_MODIFIER_FIRST_NAME)
    var lastEditorFirstName: String? = null,
    @ReadOnlyProperty
    @Column(LAST_MODIFIER_LAST_NAME)
    var lastEditorLastName: String? = null,
    @ReadOnlyProperty
    @Column(LAST_MODIFIER_EMAIL)
    var lastEditorEmail: String? = null,
)
