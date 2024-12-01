package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
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
    override fun updateSelectedEventProfile(profileId: UUID): Mono<PreferencesModel> {
        return currentUser().flatMap { service.updateUserPreferenceSelectedEventProfileById(it, profileId) }
    }
}
