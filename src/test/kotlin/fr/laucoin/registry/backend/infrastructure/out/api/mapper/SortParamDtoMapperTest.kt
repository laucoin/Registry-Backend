package fr.laucoin.registry.backend.infrastructure.out.api.mapper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_DIRECTION_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.EMAIL
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_LOGIN
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus.BAD_REQUEST
import java.util.stream.Stream
import kotlin.test.assertEquals

class SortParamDtoMapperTest {
	@ParameterizedTest
	@MethodSource("sortParamsProvider")
	fun `Should parse the sort grammar into whitelisted sort models`(
		sortParams: List<String>?,
		direction: String?,
		expected: List<SortModel<UserSortFieldEnum>>,
	) {
		// Act
		val result = SortParamDtoMapper.toSortModels(sortParams, direction, UserSortFieldEnum::fromParamName)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@ValueSource(strings = ["passwordHash", "-lastName"])
	fun `Should reject an unknown sort field with a bad request`(unknownField: String) {
		// Act
		val exception = assertThrows<RegistryException> {
			SortParamDtoMapper.toSortModels(listOf("lastName", unknownField), "ASC", UserSortFieldEnum::fromParamName)
		}

		// Assert
		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(SORT_FIELD_IS_UNKNOWN, exception.code)
		assertEquals(arrayListOf<Any?>(unknownField), exception.args)
	}

	@ParameterizedTest
	@ValueSource(strings = ["SIDEWAYS", "descending", "-"])
	fun `Should reject an unknown sort direction with a bad request`(unknownDirection: String) {
		// Act
		val exception = assertThrows<RegistryException> {
			SortParamDtoMapper.toSortModels(listOf("lastName"), unknownDirection, UserSortFieldEnum::fromParamName)
		}

		// Assert
		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(SORT_DIRECTION_IS_UNKNOWN, exception.code)
		assertEquals(arrayListOf<Any?>(unknownDirection), exception.args)
	}

	companion object {
		@JvmStatic
		fun sortParamsProvider(): Stream<Arguments> = Stream.of(
			arguments(null, null, emptyList<SortModel<UserSortFieldEnum>>()),
			arguments(emptyList<String>(), "DESC", emptyList<SortModel<UserSortFieldEnum>>()),
			arguments(listOf("lastName"), null, listOf(SortModel(LAST_NAME))),
			arguments(listOf("lastName"), "", listOf(SortModel(LAST_NAME))),
			arguments(listOf("lastName"), "ASC", listOf(SortModel(LAST_NAME))),
			arguments(listOf("email"), "DESC", listOf(SortModel(EMAIL, descending = true))),
			arguments(listOf("email"), "desc", listOf(SortModel(EMAIL, descending = true))),
			arguments(
				listOf("lastName", "lastLogin"),
				"DESC",
				listOf(SortModel(LAST_NAME, descending = true), SortModel(LAST_LOGIN, descending = true)),
			),
			arguments(listOf("", "lastName"), "ASC", listOf(SortModel(LAST_NAME))),
		)
	}
}
