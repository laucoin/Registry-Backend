package fr.laucoin.registry.backend.infrastructure.out.api.dto

import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_PAGE_NUMBER
import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ApiConst.MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

open class PageQueryDto {
	@field:Min(0, message = PAGE_NUMBER_IS_LOWER_THAN_ZERO)
	var page: Int = DEFAULT_PAGE_NUMBER

	@field:Min(1, message = PAGE_SIZE_IS_LOWER_THAN_ONE)
	@field:Max(MAX_PAGE_SIZE, message = PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE)
	var size: Int = DEFAULT_PAGE_SIZE
}
