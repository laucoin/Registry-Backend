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
    override fun updateSelectedProjectProfile(currentUser: CurrentUserModel, profileId: UUID?): Mono<PreferencesModel> {
        return service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId)
    }

    override fun updateSelectedProjectProfileWithProjectId(currentUser: CurrentUserModel, projectId: UUID): Mono<PreferencesModel> {
        return service.updateUserPreferenceSelectedProjectProfileByProjectId(currentUser, projectId)
    }
}
