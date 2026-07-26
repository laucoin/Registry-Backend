package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericProjectEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_INSIDE_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_OUTSIDE_MEMBERS_COUNT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.group.GroupFields.GROUP_TABLE
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetTime

@Table(GROUP_TABLE)
data class GroupEntity(
	@Column(GROUP_NAME)
	var name: String? = null,
	@Column(GROUP_START_AVAILABILITY_DATE)
	var startAvailabilityDate: LocalDate? = null,
	@Column(GROUP_START_AVAILABILITY_TIME)
	var startAvailabilityTime: OffsetTime? = null,
	@Column(GROUP_END_AVAILABILITY_DATE)
	var endAvailabilityDate: LocalDate? = null,
	@Column(GROUP_END_AVAILABILITY_TIME)
	var endAvailabilityTime: OffsetTime? = null,
	@ReadOnlyProperty
	@Column(GROUP_MEMBERS_COUNT)
	var members: Long? = null,
	@ReadOnlyProperty
	@Column(GROUP_INSIDE_MEMBERS_COUNT)
	var insideMembers: Long? = null,
	@ReadOnlyProperty
	@Column(GROUP_OUTSIDE_MEMBERS_COUNT)
	var outsideMembers: Long? = null,
) : GenericProjectEntity()
