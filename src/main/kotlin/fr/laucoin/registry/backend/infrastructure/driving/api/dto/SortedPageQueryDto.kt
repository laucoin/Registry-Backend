package fr.laucoin.registry.backend.infrastructure.driving.api.dto

import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum

class SortedPageQueryDto : PageQueryDto() {
	var sort: List<String>? = null
	var direction: String = SortDirectionEnum.DEFAULT.name
}
