package fr.laucoin.registry.backend.infrastructure.out.api.controller

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PreferencesReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesLanguageWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesSelectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.PreferencesThemeWriterDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

/**
 * API v2 Preferences contract (ADR 017 §3): each preference change is one
 * explicit POST action whose body carries only the value being set. The two
 * v1 profile selectors collapse into one `select-profile` action whose body
 * carries either identifier.
 */
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
		@RequestBody @Valid theme: PreferencesThemeWriterDto,
	): Mono<PreferencesReaderDto>

	@Operation(
		summary = "Save language",
		description = "Save language preferences for other devices",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/language")
	fun updateLanguage(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestBody @Valid language: PreferencesLanguageWriterDto,
	): Mono<PreferencesReaderDto>

	@Operation(
		summary = "Select active Profile",
		description = "Select the active Profile — by Profile id or by Project id; an empty body clears the selection",
	)
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/select-profile")
	fun selectProfile(
		@AuthenticationPrincipal currentUser: CurrentUserModel,
		@RequestBody @Valid selection: PreferencesSelectProfileWriterDto,
	): Mono<PreferencesReaderDto>
}
