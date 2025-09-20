package fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_DATE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_END_TIME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_NAME
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectFields.PROJECT_TABLE
import java.time.LocalDate
import java.time.OffsetTime
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
): GenericEntity()
