package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IMetadataController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ParticipantTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.PresenceStatusReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.ProjectProfileStatusReaderDtoMapper
import java.util.Locale
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class MetadataController(
    private val usableElementStatusMapper: PresenceStatusReaderDtoMapper,
    private val projectProfileStatusReaderMapper: ProjectProfileStatusReaderDtoMapper,
    private val movementTypeReaderMapper: MovementTypeReaderDtoMapper,
    private val participantTypeReaderMapper: ParticipantTypeReaderDtoMapper,
): IMetadataController {
    override fun getUsableElementStatus(locale: Locale): Flux<LabelDto> {
        return Flux.fromIterable(PresenceStatusEnum.entries)
            .map { usableElementStatusMapper.toDto(it, locale) }
    }

    override fun getProjectProfileStatus(locale: Locale): Flux<LabelDto> {
        return Flux.fromIterable(ProfileStatusEnum.entries)
            .map { projectProfileStatusReaderMapper.toDto(it, locale) }
    }

    override fun getMovementTypes(locale: Locale): Flux<LabelDto> {
        return Flux.fromIterable(MovementTypeEnum.entries)
            .map { movementTypeReaderMapper.toDto(it, locale) }
    }

    override fun getParticipantTypes(locale: Locale): Flux<LabelDto> {
        return Flux.fromIterable(ParticipantTypeEnum.entries)
            .map { participantTypeReaderMapper.toDto(it, locale) }
    }
}
