package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IPreferencesService {
    fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel>

    fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesModel>
    fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesModel>
    fun updateUserPreferenceSelectedProjectProfileById(currentUser: CurrentUserModel, profileId: UUID?): Mono<PreferencesModel>
    fun updateUserPreferenceSelectedProjectProfileByProjectId(currentUser: CurrentUserModel, projectId: UUID): Mono<PreferencesModel>
}
