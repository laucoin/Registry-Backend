package fr.laucoin.registry.backend.infrastructure.internal.web.controller

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Tag(name = "User's Preferences management", description = "API for User's Preferences-related operations")
@RequestMapping("/api/users/preferences")
interface IPreferencesController {
    @Operation(
        summary = "Change Default Profile",
        description = "Changes the Event on which default operations are performed by changing Profile."
    )
    @PatchMapping("/profile/{profileId}/select")
    fun updateSelectedEventProfile(@PathVariable profileId: UUID): Mono<PreferencesModel>
}
