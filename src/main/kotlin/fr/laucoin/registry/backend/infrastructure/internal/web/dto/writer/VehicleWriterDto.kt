package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_LICENSE_PLATE_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_LICENSE_PLATE_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_START_LATER_THAN_END
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@StartBeforeEnd(startField = "startAvailability", endField = "endAvailability", message = VEHICLE_START_LATER_THAN_END)
data class VehicleWriterDto(
    @field:NotBlank(message = VEHICLE_LICENSE_PLATE_NULL_OR_BLANK)
    @field:Size(max = 20, message = VEHICLE_LICENSE_PLATE_TOO_LONG)
    var licensePlate: String? = null,
    @field:NotBlank(message = VEHICLE_BRAND_NULL_OR_BLANK)
    @field:Size(max = 150, message = VEHICLE_BRAND_TOO_LONG)
    var brand: String? = null,
    @field:NotBlank(message = VEHICLE_MODEL_NULL_OR_BLANK)
    @field:Size(max = 150, message = VEHICLE_MODEL_TOO_LONG)
    var model: String? = null,
    @field:Valid
    var startAvailability: CustomDateTimeWriterDto? = null,
    @field:Valid
    var endAvailability: CustomDateTimeWriterDto? = null,
)
