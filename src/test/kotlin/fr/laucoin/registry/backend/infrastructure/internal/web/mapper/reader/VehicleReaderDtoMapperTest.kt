package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USABLE_ELEMENT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import java.util.Locale
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource

class VehicleReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val eventMapper: EventReaderDtoMapper = mock()
    private val mapper: VehicleReaderDtoMapper = VehicleReaderDtoMapper(translateService, eventMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert VehicleModel to VehicleReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    VehicleModel(),
                    0,
                    0,
                ),
                Arguments.of(
                    VehicleModel().apply {
                        event = EventModel()
                        status = IN
                    },
                    1,
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert VehicleModel to VehicleReaderDto`(
        vehicle: VehicleModel,
        expectedTranslation: Int,
        expectedEventCast: Int,
    ) {
        // Arrange
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn("translated")
        whenever(eventMapper.toDto(any(), any())).thenReturn(EventReaderDto())

        // Act
        val result = mapper.toDto(vehicle, Locale.getDefault())

        // Assert
        verify(translateService, times(expectedTranslation)).getMessage(
            "$USABLE_ELEMENT_STATUS_PREFIX${vehicle.status}",
            null,
            Locale.getDefault(),
        )
        verify(eventMapper, times(expectedEventCast)).toDto(vehicle.event ?: EventModel(), Locale.getDefault())

        assertEquals(vehicle.id, result.id)
        assertEquals(vehicle.licensePlate, result.licensePlate)
        assertEquals(vehicle.brand, result.brand)
        assertEquals(vehicle.model, result.model)
        assertEquals(vehicle.startAvailability, result.startAvailability)
        assertEquals(vehicle.endAvailability, result.endAvailability)
        assertEquals(vehicle.visible, result.visible)
        assertEquals(vehicle.creation, result.creation)
        assertEquals(vehicle.lastEdition, result.lastEdition)
    }
}
