package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IMetadataController
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AvailabilityStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ParticipantTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.ProjectProfileStatusReaderDtoMapper
import java.util.Locale
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class MetadataController(
	private val presenceStatusMapper: PresenceStatusReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
	private val profileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper,
	private val movementTypeReaderMapper: MovementTypeReaderDtoMapper,
	private val participantTypeReaderMapper: ParticipantTypeReaderDtoMapper,
	private val alertStatusReaderMapper: AlertStatusReaderDtoMapper,
): IMetadataController {
	override fun getPresencesStatus(locale: Locale): Flux<LabelDto> {
		return Flux.fromIterable(PresenceStatusEnum.entries)
			.map { presenceStatusMapper.toDto(it, locale) }
	}

	override fun getAvailabilitiesStatus(locale: Locale): Flux<LabelDto> {
		return Flux.fromIterable(AvailabilityStatusEnum.entries)
			.map { availabilityStatusMapper.toDto(it, locale) }
	}

	override fun getProjectProfileStatus(locale: Locale): Flux<LabelDto> {
		return Flux.fromIterable(ProfileStatusEnum.entries)
			.map { profileStatusReaderMapper.toDto(it, locale) }
	}

	override fun getMovementTypes(locale: Locale): Flux<LabelDto> {
		return Flux.fromIterable(MovementTypeEnum.entries)
			.map { movementTypeReaderMapper.toDto(it, locale) }
	}

	override fun getParticipantTypes(locale: Locale): Flux<LabelDto> {
		return Flux.fromIterable(ParticipantTypeEnum.entries)
			.map { participantTypeReaderMapper.toDto(it, locale) }
	}

	override fun getAlertStatus(locale: Locale): Flux<LabelDto> {
		return Flux.fromIterable(AlertStatusEnum.entries)
			.map { alertStatusReaderMapper.toDto(it, locale) }
	}
}
