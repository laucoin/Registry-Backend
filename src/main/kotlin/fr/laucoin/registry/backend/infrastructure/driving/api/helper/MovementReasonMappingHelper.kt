package fr.laucoin.registry.backend.infrastructure.driving.api.helper

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReasonReaderDtoMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Choisit la raison affichée d'un mouvement : sa raison propre, à défaut son activité.
 * Périmètre : Délègue la conversion aux mappers de raison et d'activité.
 * Limites : Ne copie aucune propriété lui-même.
 */
@Component
class MovementReasonMappingHelper(
	private val reasonMapper: MovementReasonReaderDtoMapper,
	private val activityReasonMapper: MovementActivityReasonReaderDtoMapper,
) {
	@Named("movementReason")
	fun movementReason(model: MovementModel): MovementReasonsReaderDto? {
		return model.reason?.let(reasonMapper::toDto) ?: model.activity?.let(activityReasonMapper::toDto)
	}
}
