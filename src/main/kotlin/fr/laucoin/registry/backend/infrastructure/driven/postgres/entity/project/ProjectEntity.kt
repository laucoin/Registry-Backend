package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_ACCEPTED_PROFILES_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_ACTIVE_ALERTS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_ACTIVITIES_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_GROUPS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_PARTICIPANTS_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectFields.PROJECT_VEHICLES_COUNT
import java.time.LocalDate
import java.time.OffsetTime
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(PROJECT_TABLE)
data class ProjectEntity(
	@Column(PROJECT_NAME)
	var name: String? = null,
	@Column(PROJECT_BEGIN_DATE)
	var beginDate: LocalDate? = null,
	@Column(PROJECT_BEGIN_TIME)
	var beginTime: OffsetTime? = null,
	@Column(PROJECT_END_DATE)
	var endDate: LocalDate? = null,
	@Column(PROJECT_END_TIME)
	var endTime: OffsetTime? = null,
	@Column(PROJECT_OPTIONS)
	var options: List<ProjectOptionEnum>? = null,
	@ReadOnlyProperty
	@Column(PROJECT_PARTICIPANTS_COUNT)
	var participants: Long? = null,
	@ReadOnlyProperty
	@Column(PROJECT_GROUPS_COUNT)
	var groups: Long? = null,
	@ReadOnlyProperty
	@Column(PROJECT_ACCEPTED_PROFILES_COUNT)
	var acceptedProfiles: Long? = null,
	@ReadOnlyProperty
	@Column(PROJECT_VEHICLES_COUNT)
	var vehicles: Long? = null,
	@ReadOnlyProperty
	@Column(PROJECT_ACTIVITIES_COUNT)
	var activities: Long? = null,
	@ReadOnlyProperty
	@Column(PROJECT_ACTIVE_ALERTS_COUNT)
	var activeAlerts: Long? = null,
): GenericEntity()
