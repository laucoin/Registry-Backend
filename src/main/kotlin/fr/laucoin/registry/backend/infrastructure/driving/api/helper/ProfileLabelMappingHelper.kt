package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Construit le libellé de statut d'un profil de projet, y compris le cas bloqué.
 * Périmètre : Traduit le statut du profil et indique, pour un profil bloqué, le statut d'origine.
 * Limites : Ne modifie aucun profil.
 */
@Component
class ProfileLabelMappingHelper(private val translateService: ITranslateService) {
	@Named("profileStatus")
	fun profileStatus(model: ProjectProfileModel): LabelDto? {
		val originalStatus = translateService.getMessage(code = "$PROJECT_PROFILE_STATUS_PREFIX${model.status}")
		if (!model.visible) return LabelDto(BLOCKED.name, blockedLabel(model, originalStatus))

		return model.status?.let { LabelDto(it.name, originalStatus) }
	}

	private fun blockedLabel(model: ProjectProfileModel, originalStatus: String): String {
		if (model.status == null) return translateService.getMessage(code = "$PROJECT_PROFILE_STATUS_PREFIX$BLOCKED")

		return translateService.getMessage(
			code = "$PROJECT_PROFILE_STATUS_PREFIX${BLOCKED}_WITH_STATUS",
			args = arrayOf(originalStatus),
		)
	}
}
