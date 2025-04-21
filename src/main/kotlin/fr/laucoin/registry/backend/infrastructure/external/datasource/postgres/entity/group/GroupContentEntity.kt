package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(GROUP_CONTENT_TABLE)
data class GroupContentEntity(
    @Id
    var id: UUID? = null,

    @Column(GROUP_CONTENT_GROUP_ID)
    var groupId: UUID? = null,

    @Column(GROUP_CONTENT_PARTICIPANT_ID)
    var participantId: UUID? = null,
    @ReadOnlyProperty
    @Column(GROUP_CONTENT_PARTICIPANT_FIRST_NAME)
    var participantFirstName: String? = null,
    @ReadOnlyProperty
    @Column(GROUP_CONTENT_PARTICIPANT_LAST_NAME)
    var participantLastName: String? = null,
    @ReadOnlyProperty
    @Column(GROUP_CONTENT_PARTICIPANT_BIRTHDAY)
    var participantBirthday: LocalDate? = null,
    @ReadOnlyProperty
    @Column(GROUP_CONTENT_PARTICIPANT_TYPE)
    var participantType: ParticipantTypeEnum? = null,
)
