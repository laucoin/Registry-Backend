package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono
import java.util.UUID

@Deprecated(
	"API v1 has no remaining Registry-Frontend consumer and is scheduled for removal; use the /api/v2 contract.",
	level = DeprecationLevel.WARNING,
)
@Tag(name = "User's Preferences management (v1, deprecated)", description = "API for User's Preferences-related operations — deprecated, scheduled for removal; use /api/v2")
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

	@Operation(
		summary = "Change Default Profile",
		description = "Changes the Project on which default operations are performed by changing Profile.",
		deprecated = true,
	)
	@PostMapping("/profile/select")
	fun updateSelectedProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(required = false) profileId: UUID?,
	): Mono<PreferencesModel>

	@Operation(
		summary = "Change Default Profile by Project id",
		description = "Changes the Project on which default operations are performed by changing Profile.",
		deprecated = true,
	)
	@PostMapping("/projects/{projectId}/profile/select")
	fun updateSelectedProjectProfileWithProjectId(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<PreferencesModel>
}
