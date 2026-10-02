package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithEntity
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * Maps a [ProjectProfileEntity] row to [UserProjectProfileModel] — the decorated-with-Project
 * variant consumed by [fr.laucoin.registry.backend.domain.service.IUserProjectProfileService],
 * whose endpoints are not scoped under a Project id. Read-only: this model is never persisted back,
 * so [fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ProjectProfileEntityMapper]
 * remains the sole read/write mapper for [fr.laucoin.registry.backend.domain.model.ProjectProfileModel].
 */
@Component
class UserProjectProfileEntityMapper(
	private val userMapper: UserEntityMapper,
	private val projectMapper: ProjectEntityMapper,
) : IEntityReaderMapper<UserProjectProfileModel, ProjectProfileEntity> {
	override fun toModel(entity: ProjectProfileEntity): UserProjectProfileModel {
		return UserProjectProfileModel().apply {
			user = mapUserEntity(entity)
			role = entity.role
			startAccess = mapCustomDateTime(entity.startAccessDate, entity.startAccessTime)
			endAccess = mapCustomDateTime(entity.endAccessDate, entity.endAccessTime)
			status = entity.status
			availabilityStatus = buildStatus()
			isFavorite = entity.isFavorite ?: false
			project = mapProjectEntity(entity)
		}.fillWithEntity(entity)
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
					isPurged = entity.isUserPurged ?: isPurged
				}
			)
		}.orElse(null)
	}

	private fun mapProjectEntity(entity: ProjectProfileEntity): ProjectModel? {
		return Optional.ofNullable(entity.projectId).map {
			projectMapper.toModel(
				ProjectEntity().apply {
					id = it
					name = entity.projectName
					beginDate = entity.projectStartDate
					beginTime = entity.projectStartTime
					endDate = entity.projectEndDate
					endTime = entity.projectEndTime
					options = entity.projectOptions
				}
			).apply { counts = projectMapper.toCountsModel(entity) }
		}.orElse(null)
	}
}
