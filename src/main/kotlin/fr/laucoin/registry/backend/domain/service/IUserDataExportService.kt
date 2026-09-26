package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserDataExportModel
import reactor.core.publisher.Mono

/**
 * Use-case entry point for the GDPR access/portability export of the caller's own account: gathers
 * the account, preferences, Project memberships, authored content across every Project, and any
 * linked Participant's data into one [UserDataExportModel].
 */
interface IUserDataExportService {
	fun exportCurrentUserData(currentUser: CurrentUserModel): Mono<UserDataExportModel>
}
