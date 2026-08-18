package fr.laucoin.registry.backend.infrastructure.out.api.mapper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_DIRECTION_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.enumeration.SortDirectionEnum
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Parses the API v2 sort grammar: `sort=field,otherField` with a
 * separate `direction=ASC|DESC`. Spring binds comma-separated and repeated
 * params into one list; the direction applies to every key, so a field name is
 * only ever a field name — the leading `-` that used to reverse one is gone,
 * and `-field` now falls through to the unknown-field refusal like any other
 * name that is not on the whitelist.
 *
 * Unknown fields and unknown directions are a 400 — neither is forwarded to SQL.
 */
object SortParamDtoMapper {
	fun <T> toSortModels(
		sortParams: List<String>?,
		direction: String?,
		fieldResolver: (String) -> T?,
	): List<SortModel<T>> {
		val descending = toDirection(direction).descending
		return sortParams.orEmpty()
			.filter { it.isNotBlank() }
			.map { paramName ->
				val field = fieldResolver(paramName)
					?: throw RegistryException(
						status = BAD_REQUEST,
						code = SORT_FIELD_IS_UNKNOWN,
						args = arrayListOf(paramName)
					)
				SortModel(field, descending)
			}
	}

	private fun toDirection(direction: String?): SortDirectionEnum {
		if (direction.isNullOrBlank()) {
			return SortDirectionEnum.DEFAULT
		}
		return SortDirectionEnum.fromParamName(direction)
			?: throw RegistryException(
				status = BAD_REQUEST,
				code = SORT_DIRECTION_IS_UNKNOWN,
				args = arrayListOf(direction)
			)
	}
}
