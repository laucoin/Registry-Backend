package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Tag(name = "User's Preferences management", description = "API for User's Preferences-related operations")
@RequestMapping("/api/users/preferences")
interface IPreferencesController {
    @Operation(
        summary = "Change Default Profile",
        description = "Changes the Event on which default operations are performed by changing Profile.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PatchMapping("/profile/{profileId}/select")
    fun updateSelectedEventProfile(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable profileId: UUID,
    ): Mono<PreferencesModel>

    @Operation(
        summary = "Change Default Profile by Event id",
        description = "Changes the Event on which default operations are performed by changing Profile.",
        parameters = [
            Parameter(
                name = ACCEPT_LANGUAGE,
                description = "Locale, used for metadata and error translation.",
                `in` = HEADER
            ),
        ],
    )
    @PatchMapping("/events/{eventId}/profile/select")
    fun updateSelectedEventProfileWithEventId(
        @AuthenticationPrincipal currentUser: CurrentUserModel,
        @PathVariable eventId: UUID,
    ): Mono<PreferencesModel>
}
