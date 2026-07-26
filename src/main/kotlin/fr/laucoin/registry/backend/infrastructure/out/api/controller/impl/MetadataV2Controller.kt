package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IMetadataV2Controller
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AvailabilityStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class MetadataV2Controller(
	private val presenceStatusMapper: PresenceStatusReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
	private val profileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper,
	private val movementTypeReaderMapper: MovementTypeReaderDtoMapper,
	private val participantTypeReaderMapper: ParticipantTypeReaderDtoMapper,
	private val alertStatusReaderMapper: AlertStatusReaderDtoMapper,
) : IMetadataV2Controller {
	override fun getPresencesStatus(): Flux<LabelDto> {
		return Flux.fromIterable(PresenceStatusEnum.entries).map(presenceStatusMapper::toDto)
	}

	override fun getAvailabilitiesStatus(): Flux<LabelDto> {
		return Flux.fromIterable(AvailabilityStatusEnum.entries).map(availabilityStatusMapper::toDto)
	}

	override fun getProjectProfileStatus(): Flux<LabelDto> {
		return Flux.fromIterable(ProfileStatusEnum.entries).map(profileStatusReaderMapper::toDto)
	}

	override fun getMovementTypes(): Flux<LabelDto> {
		return Flux.fromIterable(MovementTypeEnum.entries).map(movementTypeReaderMapper::toDto)
	}

	override fun getParticipantTypes(): Flux<LabelDto> {
		return Flux.fromIterable(ParticipantTypeEnum.entries).map(participantTypeReaderMapper::toDto)
	}

	override fun getAlertStatus(): Flux<LabelDto> {
		return Flux.fromIterable(AlertStatusEnum.entries).map(alertStatusReaderMapper::toDto)
	}
}
