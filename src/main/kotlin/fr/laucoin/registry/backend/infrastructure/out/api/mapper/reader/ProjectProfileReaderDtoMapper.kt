package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectProfileReaderDto
import org.springframework.stereotype.Component
import java.util.Objects
import java.util.Optional

@Component
class ProjectProfileReaderDtoMapper(
	private val translateService: ITranslateService,
	private val projectMapper: ProjectReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
	private val partialUserMapper: PartialUserReaderDtoMapper,
) : IGenericReaderDtoMapper<ProjectProfileModel, ProjectProfileReaderDto> {
	override fun toDto(model: ProjectProfileModel): ProjectProfileReaderDto {
		return ProjectProfileReaderDto(
			user = Optional.ofNullable(model.user).map(partialUserMapper::toDto).orElse(null),
			role = Optional.ofNullable(model.role).map {
				LabelDto(
					it,
					translateService.getMessage(code = "$PROJECT_PROFILE_ROLE_PREFIX$it"),
				)
			}.orElse(null),
			availabilityStatus = Optional.ofNullable(model.availabilityStatus)
				.map { availabilityStatusMapper.toDto(it, model.startAccess, model.endAccess) }
				.orElse(null),
			status = buildStatus(model),
			startAccess = model.startAccess,
			endAccess = model.endAccess,
			favorite = model.favorite,
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map(projectMapper::toDto).orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}

	private fun buildStatus(model: ProjectProfileModel): LabelDto? {
		val originalStatus =
			translateService.getMessage(code = "$PROJECT_PROFILE_STATUS_PREFIX${model.status}")
		if (!model.visible) {
			return LabelDto(
				BLOCKED.name,
				Optional.ofNullable(model.status).map {
					translateService.getMessage(
						code = "$PROJECT_PROFILE_STATUS_PREFIX${BLOCKED}_WITH_STATUS",
						args = arrayOf(originalStatus),
					)
				}.orElse(translateService.getMessage(code = "$PROJECT_PROFILE_STATUS_PREFIX$BLOCKED"))
			)
		}

		return if (Objects.nonNull(model.status)) LabelDto(
			model.status!!.name,
			originalStatus,
		) else null
	}
}
