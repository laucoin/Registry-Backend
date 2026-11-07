package fr.laucoin.registry.backend.infrastructure.driving.api.dto.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_MIN
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_MAX
import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PAGE_SIZE_MIN
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

open class PageQueryDto(
	@field:Min(PAGE_MIN, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO)
	@field:Max(PAGE_MAX, message = PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE)
	val page: Long,
	@field:Min(PAGE_SIZE_MIN, message = PAGE_SIZE_IS_LOWER_THAN_ONE)
	@field:Max(PAGE_SIZE_MAX, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE)
	val size: Int,
)
