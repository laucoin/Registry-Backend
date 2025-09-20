package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DELETE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_DISABLE_LAST_GROUP_MEMBER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_GROUPS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ParticipantService(
	private val projectService: IProjectService,
	private val port: IParticipantPort,
	private val userPort: IUserPort,
	private val movementPort: IMovementPort,
	private val groupPort: IGroupPort,
	@param:Value("\${registry.feature.participant.searched.max-user-result}")
	private val maxUserResult: Int,
	@param:Value("\${registry.feature.participant.searched.max-group-result}")
	private val maxGroupResult: Int,
): IParticipantService, GenericService() {
	override fun findParticipantsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> {
		return port.findPage(projectId, pageable, searchParams)
	}

	override fun findBirthdays(projectId: UUID): Flux<ParticipantModel> {
		return port.findBirthdays(projectId, visibilitySearched = true)
	}

	override fun findParticipantsByIds(
		projectId: UUID,
		ids: List<UUID>,
		visibilitySearched: Boolean?
	): Flux<ParticipantModel> {
		return port.findAllByIds(projectId, ids, visibilitySearched)
	}

	override fun findParticipantById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ParticipantModel> {
		return port.findById(projectId, id, visibilitySearched)
			.notFoundIfEmpty(id)
	}

	override fun searchUsersByText(projectId: UUID, textSearched: String?): Flux<UserModel> {
		return userPort.findWithLimit(
			maxUserResult,
			UserSearchParamModel(textSearched, visibilitySearched = true),
		)
	}

	override fun searchGroupsByText(projectId: UUID, textSearched: String?): Flux<GroupModel> {
		return groupPort.findWithLimit(
			maxGroupResult,
			projectId,
			GroupSearchParamModel(textSearched, visibilitySearched = true),
		)
	}

	override fun findParticipantMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return movementPort.findPageByParticipantId(
			projectId,
			id,
			pageable,
			searchParams
		)
	}

	override fun createParticipant(
		currentUser: CurrentUserModel,
		participant: ParticipantModel
	): Mono<ParticipantModel> {
		return projectService.validateDateTimes(
			participant.project!!.id!!,
			participant.startAvailability,
			participant.endAvailability,
			PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap {
				if (Objects.nonNull(participant.user)) {
					validateNoParticipantForUser(participant.project!!.id!!, participant.user!!.id!!)
				} else Mono.just(emptyList())
			}
			.flatMap {
				if (participant.groups.isNotEmpty()) {
					validateGroups(participant.project!!.id!!, participant, participant.groups.mapNotNull { g -> g.id })
				} else Mono.just(participant)
			}
			.flatMap { port.create(participant.apply { create(currentUser) }) }
	}

	private fun validateNoParticipantForUser(projectId: UUID, userId: UUID): Mono<List<ParticipantModel>> {
		return port.findByUserId(projectId, userId)
			.collectList()
			.handle { it, handle ->
				if (it.isNotEmpty()) {
					val exception = RegistryException(
						UNPROCESSABLE_ENTITY,
						PARTICIPANT_IN_PROJECT_ALREADY_LINKED_TO_USER,
						arrayListOf("${it.first().firstName} ${it.first().lastName}")
					)
					log.warn("Attempt to link an already link user to a participant", exception)
					handle.error(exception)
				} else handle.next(it)
			}
	}

	private fun validateGroups(
		projectId: UUID,
		participant: ParticipantModel,
		newGroupIds: List<UUID>
	): Mono<ParticipantModel> {
		return groupPort.findAllByIds(projectId, newGroupIds, visibilitySearched = null)
			.collectList()
			.handle { it, handle ->
				when {
					it.size != newGroupIds.size -> handle.error(
						RegistryException(
							NOT_FOUND,
							PARTICIPANT_GROUPS_NOT_FOUND_IN_PARTICIPANT_PROJECT,
						)
					)

					it.any { m -> m.isNotVisible() } -> handle.error(
						RegistryException(
							NOT_FOUND,
							PARTICIPANT_GROUPS_NOT_VISIBLE,
						)
					)

					else -> handle.next(participant)
				}
			}
	}

	override fun updateParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		participant: ParticipantModel
	): Mono<ParticipantModel> {
		return projectService.validateDateTimes(
			participant.project!!.id!!,
			participant.startAvailability,
			participant.endAvailability,
			PARTICIPANT_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { findParticipantById(projectId, id, visibilitySearched = null) }
			.flatMap { validateNoMovementConflict(participant, it) }
			.flatMap { toUpdate ->
				if (toUpdate.user?.id != participant.user?.id && Objects.nonNull(participant.user?.id)) {
					validateNoParticipantForUser(participant.project!!.id!!, participant.user!!.id!!)
						.map { toUpdate }
				} else {
					Mono.just(toUpdate)
				}
			}
			.flatMap {
				val newGroup: List<UUID> = it.getNewGroupIds(participant)
				if (newGroup.isEmpty()) {
					Mono.just(it)
				} else {
					validateGroups(participant.project!!.id!!, it, newGroup)
				}
			}
			.map {
				it.apply {
					firstName = participant.firstName
					lastName = participant.lastName
					birthday = participant.birthday
					groups = participant.groups
					user = participant.user
					startAvailability = participant.startAvailability
					endAvailability = participant.endAvailability
				}
			}
			.updateParticipant(currentUser)
	}

	private fun Mono<ParticipantModel>.updateParticipant(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser) })
	}

	override fun disableParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ParticipantModel> {
		return findParticipantById(projectId, id, visibilitySearched = true)
			.validateNotLastGroupMember(PARTICIPANT_DISABLE_LAST_GROUP_MEMBER)
			.updateVisibility(visibility = false)
			.updateParticipant(currentUser)
	}

	override fun enableParticipantById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<ParticipantModel> {
		return findParticipantById(projectId, id, visibilitySearched = false)
			.updateVisibility(visibility = true)
			.updateParticipant(currentUser)
	}

	override fun deleteParticipantById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
		return findParticipantById(projectId, id, visibilitySearched = null)
			.validateHasNoMovementLinked(PARTICIPANT_DELETE_HAS_MOVEMENT)
			.validateNotLastGroupMember(PARTICIPANT_DELETE_LAST_GROUP_MEMBER)
			.flatMap { port.deleteById(it.id!!) }
	}

	override fun purgeParticipantsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging participants unused since {}", dateThreshold)
		return port.findUnusedSince(dateThreshold)
			.flatMap {
				if (dryRun) {
					log.info("[Dry run] participant {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging participant {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { e -> log.info("Participant {} was deleted", e) }
						.doOnError { err -> log.error("Failed to purge participant {}", it, err) }
				}
			}
	}

	private fun validateNoMovementConflict(
		participant: ParticipantModel, oldParticipant: ParticipantModel
	): Mono<ParticipantModel> {
		val smartStartCheck = if (participant.startAvailability.asStartIsAfterOther(oldParticipant.startAvailability))
			validateNoMovementConflictBefore(participant, oldParticipant)
		else Mono.just(oldParticipant)

		val smartEndCheck = if (participant.endAvailability.asEndIsBeforeOther(oldParticipant.endAvailability))
			validateNoMovementConflictAfter(participant, oldParticipant)
		else Mono.just(oldParticipant)

		return Mono.zip(smartStartCheck, smartEndCheck).map { oldParticipant }
	}

	private fun validateNoMovementConflictBefore(
		participant: ParticipantModel, oldParticipant: ParticipantModel
	): Mono<ParticipantModel> {
		return movementPort.countAllByParticipantId(
			oldParticipant.project!!.id!!,
			oldParticipant.id!!,
			MovementSearchParamModel(
				visibilitySearched = null,
				typeSearched = null,
				endDateTimeSearched = participant.startAvailability!!.toZonedDateTime(),
			)
		).handle { it, handle ->
			if (it > 0) {
				log.warn(
					"The participant {} already has {} movement(s) before the new end date",
					oldParticipant.id,
					it
				)
				handle.error(
					RegistryException(
						UNPROCESSABLE_ENTITY,
						PARTICIPANT_OUT_OF_MOVEMENT_DATETIME,
						arrayListOf(it),
					)
				)
			} else handle.next(oldParticipant)
		}
	}

	private fun validateNoMovementConflictAfter(
		participant: ParticipantModel, oldParticipant: ParticipantModel
	): Mono<ParticipantModel> {
		return movementPort.countAllByParticipantId(
			oldParticipant.project!!.id!!,
			oldParticipant.id!!,
			MovementSearchParamModel(
				visibilitySearched = null,
				typeSearched = null,
				startDateTimeSearched = participant.endAvailability!!.toZonedDateTime(),
			)
		).handle { it, handle ->
			if (it > 0) {
				log.warn(
					"The participant {} already has {} movement(s) after the new start date",
					oldParticipant.id,
					it
				)
				handle.error(
					RegistryException(
						UNPROCESSABLE_ENTITY,
						PARTICIPANT_OUT_OF_MOVEMENT_DATETIME,
						arrayListOf(it),
					)
				)
			} else handle.next(oldParticipant)
		}
	}

	private fun Mono<ParticipantModel>.validateHasNoMovementLinked(error: String) = flatMap { participantToUpdate ->
		movementPort.countAllByParticipantId(
			participantToUpdate.project!!.id!!,
			participantToUpdate.id!!,
			MovementSearchParamModel(),
		).handle { it, handle ->
			if (it > 0) {
				log.warn("The participant {} already linked to movement(s)", participantToUpdate.id)
				handle.error(RegistryException(CONFLICT, error))
			} else handle.next(participantToUpdate)
		}
	}

	private fun Mono<ParticipantModel>.validateNotLastGroupMember(error: String) = flatMap { participantToUpdate ->
		if (participantToUpdate.groups.isEmpty()) {
			return@flatMap Mono.just(participantToUpdate)
		}

		groupPort.findAllByIds(
			participantToUpdate.project!!.id!!,
			participantToUpdate.groups.mapNotNull { it.id },
			visibilitySearched = null
		)
			.filter { it.members.size == 1 }
			.collectList()
			.handle { it, handle ->
				if (it.isNotEmpty()) {
					log.warn("The participant {} is the last member of the group(s)", participantToUpdate.id)
					handle.error(RegistryException(CONFLICT, error))
				} else handle.next(participantToUpdate)
			}
	}
}
