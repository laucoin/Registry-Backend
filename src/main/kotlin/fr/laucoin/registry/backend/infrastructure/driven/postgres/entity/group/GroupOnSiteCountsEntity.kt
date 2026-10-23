package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_IN_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_ON_SITE_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_OUT_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupFields.GROUP_OVERDUE_MEMBERS_COUNT
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

data class GroupOnSiteCountsEntity(
	@ReadOnlyProperty
	@Column(GROUP_CONTENT_GROUP_ID)
	var groupId: UUID? = null,
	@ReadOnlyProperty
	@Column(GROUP_ON_SITE_MEMBERS_COUNT)
	var onSiteMembers: Long? = null,
	@ReadOnlyProperty
	@Column(GROUP_IN_MEMBERS_COUNT)
	var inMembers: Long? = null,
	@ReadOnlyProperty
	@Column(GROUP_OUT_MEMBERS_COUNT)
	var outMembers: Long? = null,
	@ReadOnlyProperty
	@Column(GROUP_OVERDUE_MEMBERS_COUNT)
	var overdueMembers: Long? = null,
)
