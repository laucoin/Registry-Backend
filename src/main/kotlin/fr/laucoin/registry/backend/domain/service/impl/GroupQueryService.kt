package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IGroupQueryService
import java.time.Clock
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Consulte les groupes d'un projet : page, détail, membres et sélecteur de participants.
 * Périmètre : Lecture seule des groupes, avec le plafond de résultats du sélecteur.
 * Limites : Ne modifie rien.
 */
@Service
class GroupQueryService(
	private val port: IGroupPort,
	private val participantPort: IParticipantPort,
	clock: Clock,
	@param:Value($$"${registry.feature.group.searched.max-participant-result}")
	private val maxParticipantResult: Int,
) : IGroupQueryService, GenericService(clock) {
	override fun findGroupsPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: GroupSearchParamModel,
	): Mono<PageModel<GroupModel>> =
		port.findPage(projectId, pageable, searchParams)

	override fun findGroupMembersPageByGroupId(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: ParticipantSearchParamModel,
	): Mono<PageModel<ParticipantModel>> =
		participantPort.findPageByGroupId(
			projectId,
			id,
			pageable,
			searchParams,
		)

	override fun findGroupById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?,
		memberIsVisible: Boolean?,
		memberIsAvailable: Boolean?,
	): Mono<GroupModel> =
		port.findByIdWithContent(
			projectId,
			id,
			isVisible,
			memberIsVisible,
			memberIsAvailable
		)
			.notFoundIfEmpty(id)

	override fun findGroupSummaryById(projectId: UUID, id: UUID): Mono<GroupModel> =
		port.findSummaryById(projectId, id).notFoundIfEmpty(id)

	override fun searchParticipantsByText(projectId: UUID, query: String?): Flux<ParticipantModel> =
		participantPort.findWithLimit(
			maxParticipantResult,
			projectId,
			ParticipantSearchParamModel(type = REGISTERED, isVisible = true).apply {
				this.query = query
			},
		)
}
