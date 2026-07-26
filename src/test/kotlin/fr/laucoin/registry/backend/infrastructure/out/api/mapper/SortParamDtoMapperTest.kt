package fr.laucoin.registry.backend.infrastructure.out.api.mapper

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.EMAIL
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_LOGIN
import fr.laucoin.registry.backend.domain.enumeration.UserSortFieldEnum.LAST_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus.BAD_REQUEST
import java.util.stream.Stream
import kotlin.test.assertEquals

class SortParamDtoMapperTest {
	@ParameterizedTest
	@MethodSource("sortParamsProvider")
	fun `Should parse the sort grammar into whitelisted sort models`(
		sortParams: List<String>?,
		expected: List<SortModel<UserSortFieldEnum>>,
	) {
		// Act
		val result = SortParamDtoMapper.toSortModels(sortParams, UserSortFieldEnum::fromParamName)

		// Assert
		assertEquals(expected, result)
	}

	@Test
	fun `Should reject an unknown sort field with a bad request`() {
		// Act
		val exception = assertThrows<RegistryException> {
			SortParamDtoMapper.toSortModels(listOf("lastName", "-passwordHash"), UserSortFieldEnum::fromParamName)
		}

		// Assert
		assertEquals(BAD_REQUEST, exception.status)
		assertEquals(SORT_FIELD_IS_UNKNOWN, exception.code)
		assertEquals(arrayListOf<Any?>("passwordHash"), exception.args)
	}

	companion object {
		@JvmStatic
		fun sortParamsProvider(): Stream<Arguments> = Stream.of(
			arguments(null, emptyList<SortModel<UserSortFieldEnum>>()),
			arguments(emptyList<String>(), emptyList<SortModel<UserSortFieldEnum>>()),
			arguments(listOf("lastName"), listOf(SortModel(LAST_NAME))),
			arguments(listOf("-email"), listOf(SortModel(EMAIL, descending = true))),
			arguments(
				listOf("lastName", "-lastLogin"),
				listOf(SortModel(LAST_NAME), SortModel(LAST_LOGIN, descending = true)),
			),
			arguments(listOf("", "lastName"), listOf(SortModel(LAST_NAME))),
		)
	}
}
