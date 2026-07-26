package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithProjectAndModel
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class ProjectProfileEntityMapper(
	private val userMapper: UserEntityMapper,
) : IEntityMapper<ProjectProfileModel, ProjectProfileEntity> {
	override fun toModel(entity: ProjectProfileEntity): ProjectProfileModel {
		return ProjectProfileModel().apply {
			user = mapUserEntity(entity)
			role = entity.role
			startAccess = mapCustomDateTime(entity.startAccessDate, entity.startAccessTime)
			endAccess = mapCustomDateTime(entity.endAccessDate, entity.endAccessTime)
			status = entity.status
			favorite = entity.favorite
			availabilityStatus = buildStatus()
		}.fillWithProjectAndEntity(entity)
	}

	private fun mapUserEntity(entity: ProjectProfileEntity): UserModel? {
		return Optional.ofNullable(entity.userId).map {
			userMapper.toModel(
				UserEntity().apply {
					id = it
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
			favorite = model.favorite
		}.fillWithProjectAndModel(model)
	}
}
