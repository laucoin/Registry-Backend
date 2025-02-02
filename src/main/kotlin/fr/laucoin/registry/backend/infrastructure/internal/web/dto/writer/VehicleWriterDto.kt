package fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer

import fr.laucoin.registry.backend.domain.annotation.StartBeforeEnd
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_BRAND_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_MODEL_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_REGISTRATION_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_REGISTRATION_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.VehicleError.VEHICLE_START_LATER_THAN_END
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

@StartBeforeEnd(startField = "begin", endField = "end", message = VEHICLE_START_LATER_THAN_END)
data class VehicleWriterDto(
    @field:NotBlank(message = VEHICLE_REGISTRATION_NULL_OR_BLANK)
    @field:Size(max = 20, message = VEHICLE_REGISTRATION_TOO_LONG)
    var registration: String? = null,
    @field:NotBlank(message = VEHICLE_BRAND_NULL_OR_BLANK)
    @field:Size(max = 150, message = VEHICLE_BRAND_TOO_LONG)
    var brand: String? = null,
    @field:NotBlank(message = VEHICLE_MODEL_NULL_OR_BLANK)
    @field:Size(max = 150, message = VEHICLE_MODEL_TOO_LONG)
    var model: String? = null,
    var begin: ZonedDateTime? = null,
    var end: ZonedDateTime? = null,
)
