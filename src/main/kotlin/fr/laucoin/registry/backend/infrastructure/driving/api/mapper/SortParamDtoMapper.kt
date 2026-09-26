package fr.laucoin.registry.backend.infrastructure.driving.api.mapper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_DIRECTION_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Component

/**
 * Parses the v2 sort grammar: `sort=field,otherField` with a separate
 * `direction=ASC|DESC` applying to every listed field. Unknown fields and
 * unknown directions are refused with a 400 rather than forwarded to SQL —
 * [fieldResolver] is the per-resource whitelist.
 */
@Component
class SortParamDtoMapper {
	fun <T> toSortModels(
		sortParams: List<String>?,
		direction: String,
		fieldResolver: (String) -> T?,
	): List<SortModel<T>> {
		val resolvedDirection = toDirection(direction)
		return sortParams.orEmpty()
			.filter { it.isNotBlank() }
			.map { paramName ->
				val field = fieldResolver(paramName)
					?: throw RegistryException(
						status = BAD_REQUEST,
						code = SORT_FIELD_IS_UNKNOWN,
						args = arrayListOf(paramName),
					)
				SortModel(field, resolvedDirection)
			}
	}

	private fun toDirection(direction: String): SortDirectionEnum {
		return SortDirectionEnum.entries.firstOrNull { it.name.equals(direction, ignoreCase = true) }
			?: throw RegistryException(
				status = BAD_REQUEST,
				code = SORT_DIRECTION_IS_UNKNOWN,
				args = arrayListOf(direction),
			)
	}
}
