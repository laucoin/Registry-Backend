package fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.VEHICLE_BRAND_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.VEHICLE_LICENSE_PLATE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.VEHICLE_MODEL_MAX
import fr.laucoin.registry.backend.infrastructure.driving.api.validation.annotation.StartBeforeEnd
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
	@field:Size(max = VEHICLE_LICENSE_PLATE_MAX, message = VEHICLE_LICENSE_PLATE_TOO_LONG)
	var licensePlate: String? = null,
	@field:NotBlank(message = VEHICLE_BRAND_NULL_OR_BLANK)
	@field:Size(max = VEHICLE_BRAND_MAX, message = VEHICLE_BRAND_TOO_LONG)
	var brand: String? = null,
	@field:NotBlank(message = VEHICLE_MODEL_NULL_OR_BLANK)
	@field:Size(max = VEHICLE_MODEL_MAX, message = VEHICLE_MODEL_TOO_LONG)
	var model: String? = null,
	@field:Valid
	var startAvailability: CustomDateTimeWriterDto? = null,
	@field:Valid
	var endAvailability: CustomDateTimeWriterDto? = null,
)
