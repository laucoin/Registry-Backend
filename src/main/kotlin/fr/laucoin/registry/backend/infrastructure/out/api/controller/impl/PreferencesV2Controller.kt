package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TYPE_MISMATCH
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IPreferencesV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PreferencesReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesLanguageWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesSelectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesThemeWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PreferencesReaderDtoMapper
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.Objects

@RestController
class PreferencesV2Controller(
	private val service: IPreferencesService,
	private val readerMapper: PreferencesReaderDtoMapper,
) : IPreferencesV2Controller {
	override fun updateTheme(
		currentUser: CurrentUserModel,
		theme: PreferencesThemeWriterDto,
	): Mono<PreferencesReaderDto> {
		return service.updateTheme(currentUser, theme.theme!!).map(readerMapper::toDto)
	}

	override fun updateLanguage(
		currentUser: CurrentUserModel,
		language: PreferencesLanguageWriterDto,
	): Mono<PreferencesReaderDto> {
		return service.updateLanguage(currentUser, language.language!!).map(readerMapper::toDto)
	}

	override fun selectProfile(
		currentUser: CurrentUserModel,
		selection: PreferencesSelectProfileWriterDto,
	): Mono<PreferencesReaderDto> {
		if (Objects.nonNull(selection.profileId) && Objects.nonNull(selection.projectId)) {
			throw RegistryException(status = BAD_REQUEST, code = PARAMETER_TYPE_MISMATCH)
		}

		val preferences = selection.projectId
			?.let { service.updateUserPreferenceSelectedProjectProfileByProjectId(currentUser, it) }
			?: service.updateUserPreferenceSelectedProjectProfileById(currentUser, selection.profileId)

		return preferences.map(readerMapper::toDto)
	}
}
