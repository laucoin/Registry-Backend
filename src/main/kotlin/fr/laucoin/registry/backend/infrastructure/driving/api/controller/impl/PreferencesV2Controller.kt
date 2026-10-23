package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IPreferencesV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferenceReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PreferenceReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class PreferencesV2Controller(
	private val service: IPreferencesService,
	private val mapper: PreferenceReaderDtoMapper,
): IPreferencesV2Controller {
	override fun updateTheme(
		currentUser: CurrentUserModel,
		theme: ThemeEnum
	): Mono<PreferenceReaderDto> {
		return service.updateTheme(currentUser, theme).map(mapper::toDto)
	}

	override fun updateLanguage(
		currentUser: CurrentUserModel,
		language: String
	): Mono<PreferenceReaderDto> {
		return service.updateLanguage(currentUser, language).map(mapper::toDto)
	}

	override fun updateRefreshMode(
		currentUser: CurrentUserModel,
		mode: RefreshModeEnum
	): Mono<PreferenceReaderDto> {
		return service.updateRefreshMode(currentUser, mode).map(mapper::toDto)
	}
}
