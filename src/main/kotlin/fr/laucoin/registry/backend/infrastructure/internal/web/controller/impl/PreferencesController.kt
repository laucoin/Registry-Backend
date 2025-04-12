package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IPreferencesController
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class PreferencesController(
    private val service: IPreferencesService,
): IPreferencesController {
    override fun updateSelectedEventProfile(currentUser: CurrentUserModel, profileId: UUID): Mono<PreferencesModel> {
        return service.updateUserPreferenceSelectedEventProfileById(currentUser, profileId)
    }

    override fun updateSelectedEventProfileWithEventId(currentUser: CurrentUserModel, eventId: UUID): Mono<PreferencesModel> {
        return service.updateUserPreferenceSelectedEventProfileByEventId(currentUser, eventId)
    }
}
