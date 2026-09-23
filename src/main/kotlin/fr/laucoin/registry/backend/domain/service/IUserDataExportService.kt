package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.UserDataExportModel
import reactor.core.publisher.Mono

interface IUserDataExportService {
	fun exportCurrentUserData(currentUser: CurrentUserModel): Mono<UserDataExportModel>
}
