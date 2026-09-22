package fr.laucoin.registry.backend.infrastructure.driving.api.controller

import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ApiConst.API_V2
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum.SENSITIVE
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferencesReaderDto
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

@Tag(name = "User's Preferences management", description = "API for User's Preferences-related operations")
@RequestMapping("$API_V2/users/preferences")
interface IPreferencesV2Controller {
	@Operation(
		summary = "Save theme",
		description = "Save theme preferences for other devices",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/theme")
	fun updateTheme(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(required = true) theme: ThemeEnum,
	): Mono<PreferencesReaderDto>

	@Operation(
		summary = "Save language",
		description = "Save language preferences for other devices",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/language")
	fun updateLanguage(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@Parameter(description = "Language code, e.g. 'en', 'en-US', 'fr', 'fr-FR', etc.")
		@RequestParam(required = true) language: String,
	): Mono<PreferencesReaderDto>

	@Operation(
		summary = "Change Default Profile",
		description = "Changes the Project on which default operations are performed by changing Profile.",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/profile/select")
	fun updateSelectedProjectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestParam(required = false) profileId: UUID?,
	): Mono<PreferencesReaderDto>

	@Operation(
		summary = "Change Default Profile by Project id",
		description = "Changes the Project on which default operations are performed by changing Profile.",
	)
	@RateLimited(SENSITIVE)
	@PostMapping("/projects/{projectId}/profile/select")
	fun updateSelectedProjectProfileWithProjectId(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@PathVariable projectId: UUID,
	): Mono<PreferencesReaderDto>
}
