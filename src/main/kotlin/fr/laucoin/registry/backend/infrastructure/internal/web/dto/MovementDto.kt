package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

data class MovementDto(
    @field:NotNull(message = MOVEMENT_DATETIME_NULL)
    var dateTime: ZonedDateTime?,
    @field:NotNull(message = MOVEMENT_TYPE_NULL)
    var type: MovementTypeEnum?,
    @field:Valid
    @field:NotEmpty(message = MOVEMENT_CONTENT_EMPTY)
    var content: List<MovementContentDto>?,
)
