package fr.laucoin.registry.backend.infrastructure.out.api.mapper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Parses the API v2 sort grammar (ADR 017 §5): `sort=field,-otherField`.
 * Spring binds comma-separated and repeated params into one list; a leading
 * `-` means descending. Unknown fields are a 400 — never forwarded to SQL.
 */
object SortParamDtoMapper {
	fun <T> toSortModels(sortParams: List<String>?, fieldResolver: (String) -> T?): List<SortModel<T>> {
		return sortParams.orEmpty()
			.filter { it.isNotBlank() }
			.map { param ->
				val descending = param.startsWith("-")
				val paramName = if (descending) param.substring(1) else param
				val field = fieldResolver(paramName)
					?: throw RegistryException(
						status = BAD_REQUEST,
						code = SORT_FIELD_IS_UNKNOWN,
						args = arrayListOf(paramName)
					)
				SortModel(field, descending)
			}
	}
}
