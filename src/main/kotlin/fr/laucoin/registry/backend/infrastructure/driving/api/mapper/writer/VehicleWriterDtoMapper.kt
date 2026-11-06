package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.UUID
import org.mapstruct.Named
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [CustomDateTimeWriterDtoMapper::class])
interface VehicleWriterDtoMapper: IGenericProjectWriterDtoMapper<VehicleModel, VehicleWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "licensePlate", source = "dto.licensePlate", qualifiedByName = ["uppercase"])
	@Mapping(target = "brand", source = "dto.brand")
	@Mapping(target = "model", source = "dto.model")
	@Mapping(target = "startAvailability", source = "dto.startAvailability")
	@Mapping(target = "endAvailability", source = "dto.endAvailability")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: VehicleWriterDto, projectId: UUID): VehicleModel

	@Named("uppercase")
	fun uppercase(value: String): String = value.uppercase()
}
