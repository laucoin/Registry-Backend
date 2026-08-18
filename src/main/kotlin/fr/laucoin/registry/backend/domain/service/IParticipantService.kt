package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ParticipantSortFieldEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.UserModel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface IParticipantService {
	fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
		sort: List<SortModel<ParticipantSortFieldEnum>> = emptyList(),
	): Mono<PageModel<ParticipantModel>>

	fun findBirthdays(projectId: UUID, limit: Int): Flux<ParticipantModel>
	fun findArrivalsToday(projectId: UUID, limit: Int): Mono<Pair<List<ParticipantModel>, List<GroupModel>>>
	fun findDeparturesToday(projectId: UUID, limit: Int): Mono<Pair<List<ParticipantModel>, List<GroupModel>>>

	fun findParticipantsByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ParticipantModel>
	fun findParticipantById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ParticipantModel>
	fun searchUsersByText(projectId: UUID, textSearched: String?): Flux<UserModel>
	fun searchGroupsByText(projectId: UUID, textSearched: String?): Flux<GroupModel>

	fun findParticipantMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel,
	): Mono<PageModel<MovementModel>>

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
