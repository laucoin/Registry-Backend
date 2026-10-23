package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.service.IProjectService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IMetadataV2Controller
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertCriticalityMetadataReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertCriticalityMetadataReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AlertStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AvailabilityStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: REST adapter of the metadata endpoints.
 * Scope: Maps request parameters and DTOs, and delegates to the metadata use cases.
 * Limits: Holds no business rule; authorization is declared on the contract.
 */
@RestController
class MetadataV2Controller(
	private val presenceStatusMapper: PresenceStatusReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
	private val profileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper,
	private val movementTypeReaderMapper: MovementTypeReaderDtoMapper,
	private val participantTypeReaderMapper: ParticipantTypeReaderDtoMapper,
	private val alertStatusReaderMapper: AlertStatusReaderDtoMapper,
	private val alertCriticalityMetadataReaderMapper: AlertCriticalityMetadataReaderDtoMapper,
	private val projectService: IProjectService,
	private val projectOptionsReaderMapper: ProjectOptionsReaderDtoMapper,
): IMetadataV2Controller {
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

	override fun getAlertCriticality(): Mono<AlertCriticalityMetadataReaderDto> {
		return Mono.fromSupplier { alertCriticalityMetadataReaderMapper.toDto(AlertCriticalityEnum.entries) }
	}

	override fun getAvailableProjectOptions(): Flux<ProjectOptionsReaderDto> {
		return projectService.availableProjectOptions().map(projectOptionsReaderMapper::toDto)
	}
}
