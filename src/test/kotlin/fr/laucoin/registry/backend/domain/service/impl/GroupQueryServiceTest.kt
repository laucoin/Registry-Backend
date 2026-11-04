package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_FOUND_WITH_GIVEN_IDENTIFIER
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt.defaultGroup
import fr.laucoin.registry.backend.test.ModelExt.defaultGroupId
import fr.laucoin.registry.backend.test.ModelExt.defaultParticipantId
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock

import fr.laucoin.registry.backend.domain.service.IGroupQueryService

class GroupQueryServiceTest {
	private val port: IGroupPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val service: IGroupQueryService =
		GroupQueryService(port, participantPort, Clock.systemUTC(), MAX_PARTICIPANTS)


	private companion object {
		private const val MAX_PARTICIPANTS = 1
	}


	@Test
	fun `Should findGroupsPage call port findPage`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = GroupSearchParamModel()

		whenever(port.findPage(any(), any(), any()))
			.thenReturn(Mono.just(PageModel(1, 2, 3, 4, emptyList())))

		// Act
		service.findGroupsPage(defaultProjectId, pageable, params).block()

		// Assert
		verify(port).findPage(defaultProjectId, pageable, params)
	}

	@Test
	fun `Should findGroupMembersPageByGroupId call port findPageByGroupId`() {
		// Arrange
		val pageable = PageableModel(0, 10)
		val params = ParticipantSearchParamModel()

		whenever(participantPort.findPageByGroupId(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(1, 2, 3, 4, emptyList()))
		)

		// Act
		service.findGroupMembersPageByGroupId(defaultProjectId, defaultParticipantId, pageable, params).block()

		// Assert
		verify(participantPort).findPageByGroupId(defaultProjectId, defaultParticipantId, pageable, params)
	}

	@Test
	fun `Should findGroupById call port findById`() {
		// Arrange
		val onlyVisible = true
		val memberIsAvailable = null
		val memberIsVisible = null

		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.just(defaultGroup()))

		// Act
		service.findGroupById(defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable)
			.block()

		// Assert
		verify(port).findByIdWithContent(
			defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable
		)
	}

	@Test
	fun `Should findGroupById call port findById throw on empty result`() {
		// Arrange
		val onlyVisible = true
		val memberIsAvailable = true
		val memberIsVisible = true

		whenever(port.findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findGroupById(defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable)
				.block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
		assertEquals(NOT_FOUND_WITH_GIVEN_IDENTIFIER, result.message)
		assertEquals(1, result.args?.size)
		assertEquals(defaultGroupId.toString(), result.args?.first())

		verify(port).findByIdWithContent(
			defaultProjectId, defaultGroupId, onlyVisible, memberIsVisible, memberIsAvailable
		)
	}

	@Test
	fun `Should searchParticipants call port findWithLimit`() {
		// Arrange
		val query = "text"
		val expectedSearch = ParticipantSearchParamModel(null, REGISTERED, isVisible = true).apply {
			this.query = query
		}

		whenever(participantPort.findWithLimit(any(), any(), any())).thenReturn(Flux.empty())

		// Act
		service.searchParticipantsByText(defaultProjectId, query).blockFirst()

		// Assert
		verify(participantPort).findWithLimit(MAX_PARTICIPANTS, defaultProjectId, expectedSearch)
	}

	@Test
	fun `Should findGroupSummaryById read the group without its members`() {
		// Arrange
		whenever(port.findSummaryById(any(), any())).thenReturn(Mono.just(defaultGroup()))

		// Act
		val result = service.findGroupSummaryById(defaultProjectId, defaultGroupId).block()

		// Assert
		assertEquals(defaultGroupId, result?.id)
		verify(port).findSummaryById(defaultProjectId, defaultGroupId)
		verify(port, never()).findByIdWithContent(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())
	}

	@Test
	fun `Should findGroupSummaryById throw when the group does not exist`() {
		// Arrange
		whenever(port.findSummaryById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = Exceptions.unwrap(assertThrows(Exception::class.java) {
			service.findGroupSummaryById(defaultProjectId, defaultGroupId).block()
		}) as RegistryException

		// Assert
		assertEquals(NOT_FOUND, result.status)
	}
}
