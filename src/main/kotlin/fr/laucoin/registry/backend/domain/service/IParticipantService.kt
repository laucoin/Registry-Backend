package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantDataExportModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Use-case entry point for Participants: search/read, its Movement history, dashboard queries
 * (birthdays, arriving/departing today), the picker searches used to link a User or a Group, GDPR
 * data export, the create/update/disable/enable/delete lifecycle, and the retention purge sweep.
 * Callers go through this contract, never the [IParticipantPort] directly.
 */
interface IParticipantService {
	fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
		sortFields: List<SortModel<ParticipantSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID, limit: Int = 1000): Flux<ParticipantModel>
	fun findArrivingToday(projectId: UUID, limit: Int): Flux<ParticipantModel>
	fun findDepartingToday(projectId: UUID, limit: Int): Flux<ParticipantModel>

	fun findParticipantById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ParticipantModel>
	fun searchUsersByText(projectId: UUID, query: String?): Flux<UserModel>
	fun searchGroupsByText(projectId: UUID, query: String?): Flux<GroupModel>

	fun findParticipantMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

	fun exportParticipantData(projectId: UUID, id: UUID): Mono<ParticipantDataExportModel>

	fun createParticipant(currentUser: CurrentUserModel, participant: ParticipantModel): Mono<ParticipantModel>
	fun updateParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		participant: ParticipantModel
	): Mono<ParticipantModel>

	fun disableParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantModel>
	fun enableParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ParticipantModel>
	fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit>
	fun purgeParticipantsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
