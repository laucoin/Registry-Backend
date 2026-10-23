package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import org.springframework.security.access.prepost.PreAuthorize
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PREFERENCES_LANGUAGE_MAX
import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferenceReaderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "User's Preferences management", description = "API for User's Preferences-related operations")
@RequestMapping("/api/v2/users/preferences")
interface IPreferencesV2Controller {
	@Operation(
		summary = "Save theme",
		description = "Save theme preferences for other devices",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/theme")
	fun updateTheme(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(required = true) theme: ThemeEnum,
	): Mono<PreferenceReaderDto>

	@Operation(
		summary = "Save language",
		description = "Save language preferences for other devices",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/language")
	fun updateLanguage(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@Parameter(description = "Language code, e.g. 'en', 'en-US', 'fr', 'fr-FR', etc.")
		@RequestParam(required = true) @Valid @Size(max = PREFERENCES_LANGUAGE_MAX, message = PARAMETER_TOO_LONG) language: String,
	): Mono<PreferenceReaderDto>

	@Operation(
		summary = "Save refresh mode",
		description = "Save automatic data refresh frequency preferences for other devices",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/refresh-mode")
	fun updateRefreshMode(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(required = true) mode: RefreshModeEnum,
	): Mono<PreferenceReaderDto>
}
