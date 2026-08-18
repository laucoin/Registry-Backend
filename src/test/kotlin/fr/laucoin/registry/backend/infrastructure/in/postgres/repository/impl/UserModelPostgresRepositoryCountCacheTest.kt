package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.UserSearchParamModel
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.CurrentUserEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper.UserEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.IUserEntityRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserModelPostgresRepositoryCountCacheTest {
	private val entityRepository: IUserEntityRepository = mock()
	private val mapper: UserEntityMapper = mock()
	private val currentUserMapper: CurrentUserEntityMapper = mock()
	private val databaseClient: DatabaseClient = mock()
	private val converter: MappingR2dbcConverter = mock()
	private val repository = UserModelPostgresRepository(
		entityRepository, mapper, currentUserMapper, databaseClient, converter, countCacheTtlSeconds = 60,
	)

	private val pageable = PageableModel(0, 20)

	@Test
	fun `Should count once per criteria within the TTL window`() {
		// Arrange
		whenever(entityRepository.findAll(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Flux.empty())
		whenever(entityRepository.countAll(anyOrNull(), anyOrNull())).thenReturn(Mono.just(5L))

		// Act
		repository.findPage(pageable, UserSearchParamModel()).block()
		repository.findPage(pageable, UserSearchParamModel()).block()
		repository.findPage(pageable, UserSearchParamModel(textSearched = "john")).block()

		// Assert
		verify(entityRepository, times(2)).countAll(anyOrNull(), anyOrNull())
	}

	@Test
	fun `Should evict the cached counts on a user write`() {
		// Arrange
		whenever(entityRepository.findAll(anyOrNull(), anyOrNull(), any(), any())).thenReturn(Flux.empty())
		whenever(entityRepository.countAll(anyOrNull(), anyOrNull())).thenReturn(Mono.just(5L))
		val entity: UserEntity = mock()
		whenever(mapper.toEntity(any())).thenReturn(entity)
		whenever(mapper.toModel(any<UserEntity>())).thenReturn(UserModel())
		whenever(entityRepository.save(any())).thenReturn(Mono.just(entity))
		repository.findPage(pageable, UserSearchParamModel()).block()

		// Act
		repository.create(UserModel()).block()
		repository.findPage(pageable, UserSearchParamModel()).block()

		// Assert
		verify(entityRepository, times(2)).countAll(anyOrNull(), anyOrNull())
	}
}
