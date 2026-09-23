package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Deprecated(
	message = "Superseded by /api/v2/users/preferences.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "User's Preferences management", description = "API for User's Preferences-related operations")
@RequestMapping("/api/v1/users/preferences")
interface IPreferencesV1Controller {
	@Operation(
		summary = "Save theme",
		description = "Save theme preferences for other devices",
		deprecated = true,
	)
	@PostMapping("/theme")
	fun updateTheme(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(required = true) theme: ThemeEnum,
	): Mono<PreferencesModel>

	@Operation(
		summary = "Save language",
		description = "Save language preferences for other devices",
		deprecated = true,
	)
	@PostMapping("/language")
	fun updateLanguage(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@Parameter(description = "Language code, e.g. 'en', 'en-US', 'fr', 'fr-FR', etc.")
		@RequestParam(required = true) language: String,
	): Mono<PreferencesModel>

}
