package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals

class UserReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val mapper: UserReaderDtoMapper = UserReaderDtoMapper(translateService)

	private companion object {
		@JvmStatic
		fun `Should toDto convert UserModel to UserReaderDto`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					UserModel().apply {
						id = UUID.randomUUID()
						firstName = "John"
						lastName = "DOE"
						email = "john.doe@test.com"
						role = "ADMIN"
						birthday = LocalDate.now()
						lastLogin = ZonedDateTime.now()
					},
					1,
				),
				Arguments.of(
					UserModel().apply {
						id = UUID.randomUUID()
						firstName = "John"
						lastName = "DOE"
						email = "john.doe@test.com"
						birthday = LocalDate.now()
						lastLogin = ZonedDateTime.now()
					},
					0,
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toDto convert UserModel to UserReaderDto`(
		user: UserModel,
		expectedRoleTranslation: Int,
	) {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("Administrator")

		// Act
		val result = mapper.toDto(user)

		// Assert
		verify(translateService, times(expectedRoleTranslation)).getMessage(
			"${USER_ROLE_PREFIX}ADMIN",
		)

		assertEquals(user.id, result.id)
		assertEquals(user.firstName, result.firstName)
		assertEquals(user.lastName, result.lastName)
		assertEquals(user.email, result.email)
		assertEquals(user.role, result.role?.value)
		assertEquals(user.birthday, result.birthday)
		assertEquals(user.lastLogin, result.lastLogin)
	}
}
