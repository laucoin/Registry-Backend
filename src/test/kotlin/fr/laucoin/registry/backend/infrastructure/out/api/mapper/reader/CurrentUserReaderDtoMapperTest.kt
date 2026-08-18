package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserPreferencesReaderDto
import fr.laucoin.registry.backend.test.ModelExt.userId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals

class CurrentUserReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val preferenceMapper: CurrentUserPreferencesReaderDtoMapper = mock()
	private val mapper = CurrentUserReaderDtoMapper(translateService, preferenceMapper)

	private companion object {
		private const val TRANSLATED = "TRANSLATED"
		private val now = ZonedDateTime.now()
		private val preferenceDto = CurrentUserPreferencesReaderDto()

		private val model = CurrentUserModel(
			authorities = mutableListOf(SimpleGrantedAuthority("USER_AUTHORITY")),
			preferences = PreferencesModel(),
		).apply {
			firstName = "John"
			lastName = "DOE"
			email = "john.doe@test.com"
			birthday = LocalDate.of(1980, 1, 1)
			lastLogin = now
			role = "ROLE"
			id = userId
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		private val dto = CurrentUserReaderDto(
			authorities = listOf("USER_AUTHORITY"),
			preferences = preferenceDto,
			firstName = "John",
			lastName = "DOE",
			email = "john.doe@test.com",
			birthday = LocalDate.of(1980, 1, 1),
			lastLogin = now,
			role = LabelDto(value = "ROLE", label = TRANSLATED),
		).apply {
			id = userId
			visible = true
			creation = HistoryModel()
			lastEdition = HistoryModel()
		}

		@JvmStatic
		fun `CurrentUserModel to CurrentUserReaderDto data`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(model, dto, 1, 1),
				Arguments.of(
					CurrentUserModel().apply { lastLogin = now },
					CurrentUserReaderDto(authorities = emptyList(), lastLogin = now),
					0,
					0,
				),
			)
		}
	}

	@BeforeEach
	fun setup() {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn(TRANSLATED)
		whenever(preferenceMapper.toDto(any())).thenReturn(preferenceDto)
	}

	@ParameterizedTest
	@MethodSource("CurrentUserModel to CurrentUserReaderDto data")
	fun `Should toDto convert CurrentUserModel to CurrentUserReaderDto`(
		model: CurrentUserModel,
		dto: CurrentUserReaderDto,
		expectedRoleTranslation: Int,
		expectedPreferencesCast: Int,
	) {
		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(dto, result)

		verify(translateService, times(expectedRoleTranslation))
			.getMessage("${USER_ROLE_PREFIX}ROLE")

		verify(preferenceMapper, times(expectedPreferencesCast))
			.toDto(model.preferences ?: PreferencesModel())
	}

	@ParameterizedTest
	@MethodSource("CurrentUserModel to CurrentUserReaderDto data")
	fun `Should toDto convert CurrentUserModel list to CurrentUserReaderDto list`(
		model: CurrentUserModel,
		dto: CurrentUserReaderDto,
		expectedRoleTranslation: Int,
		expectedPreferencesCast: Int,
	) {
		// Arrange
		val models = listOf(model)
		val dtos = listOf(dto)

		// Act
		val result = mapper.toDtoList(models)

		// Assert
		assertEquals(dtos, result)

		verify(translateService, times(expectedRoleTranslation))
			.getMessage("${USER_ROLE_PREFIX}ROLE")

		verify(preferenceMapper, times(expectedPreferencesCast))
			.toDto(model.preferences ?: PreferencesModel())
	}

	@ParameterizedTest
	@MethodSource("CurrentUserModel to CurrentUserReaderDto data")
	fun `Should toDto convert CurrentUserModel page to CurrentUserReaderDto page`(
		model: CurrentUserModel,
		dto: CurrentUserReaderDto,
		expectedRoleTranslation: Int,
		expectedPreferencesCast: Int,
	) {
		// Arrange
		val modelPage = PageModel(
			pageNumber = 0,
			pageSize = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(model),
		)
		val dtoPage = PageModel(
			pageNumber = 0,
			pageSize = 10,
			totalPages = 1,
			totalElements = 1,
			content = listOf(dto),
			lastRefresh = modelPage.lastRefresh,
		)

		// Act
		val result = mapper.toDtoPage(modelPage)

		// Assert
		assertEquals(dtoPage, result)

		verify(translateService, times(expectedRoleTranslation))
			.getMessage("${USER_ROLE_PREFIX}ROLE")

		verify(preferenceMapper, times(expectedPreferencesCast))
			.toDto(model.preferences ?: PreferencesModel())
	}
}
