package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IPreferencesV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferencesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PreferencesReaderDtoMapper
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class PreferencesV2Controller(
	private val service: IPreferencesService,
	private val readerMapper: PreferencesReaderDtoMapper,
): IPreferencesV2Controller {
	override fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesReaderDto> {
		return service.updateTheme(currentUser, theme).map(readerMapper::toDto)
	}

	override fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesReaderDto> {
		return service.updateLanguage(currentUser, language).map(readerMapper::toDto)
	}

	override fun updateSelectedProjectProfile(currentUser: CurrentUserModel, profileId: UUID?): Mono<PreferencesReaderDto> {
		return service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId).map(readerMapper::toDto)
	}

	override fun updateSelectedProjectProfileWithProjectId(currentUser: CurrentUserModel, projectId: UUID): Mono<PreferencesReaderDto> {
		return service.updateUserPreferenceSelectedProjectProfileByProjectId(currentUser, projectId).map(readerMapper::toDto)
	}
}
