package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import java.time.Clock
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ProjectProfileEntityMapper(
	private val userMapper: UserEntityMapper,
	private val clock: Clock,
): IEntityMapper<ProjectProfileModel, ProjectProfileEntity> {
	override fun toModel(entity: ProjectProfileEntity): ProjectProfileModel {
		return ProjectProfileModel().apply {
			user = mapUserEntity(entity)
			project = mapProject(entity)
			role = entity.role
			startAccess = mapCustomDateTime(entity.startAccessDate, entity.startAccessTime)
			endAccess = mapCustomDateTime(entity.endAccessDate, entity.endAccessTime)
			status = entity.status
			availabilityStatus = buildStatus(clock)
		}.fillWithProjectAndEntity(entity)
	}

	private fun mapProject(entity: ProjectProfileEntity): ProjectModel? {
		if (Objects.isNull(entity.projectName)) return null
		return ProjectModel().apply {
			id = entity.projectId
			name = entity.projectName
			begin = mapCustomDateTime(entity.projectStartDate, entity.projectStartTime)
			end = mapCustomDateTime(entity.projectEndDate, entity.projectEndTime)
			options = entity.projectOptions
		}
	}

	private fun mapUserEntity(entity: ProjectProfileEntity): UserModel? {
		return Optional.ofNullable(entity.userId).map {
			userMapper.toModel(
				UserEntity().apply {
					id = it
					oidcId = entity.userOidcId
					firstName = entity.userFirstName
					lastName = entity.userLastName
					email = entity.userEmail
					lastLogin = entity.userLastLogin
				}
			)
		}.orElse(null)
	}

	override fun toEntity(model: ProjectProfileModel): ProjectProfileEntity {
		return ProjectProfileEntity().apply {
			userId = model.user?.id
			role = model.role
			status = model.status
			startAccessDate = model.startAccess?.date
			startAccessTime = model.startAccess?.time
			endAccessDate = model.endAccess?.date
			endAccessTime = model.endAccess?.time
		}.fillWithProjectAndModel(model)
	}
}
