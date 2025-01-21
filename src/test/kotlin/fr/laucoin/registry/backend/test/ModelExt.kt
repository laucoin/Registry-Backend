package fr.laucoin.registry.backend.test

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import java.time.ZonedDateTime.now
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

object ModelExt {

    val eventId: UUID = UUID.randomUUID()

    fun <T> PageDto<T>.assertPage(
        expectedTotalElements: Int,
        expectedOffset: Int = 0,
        expectedLimit: Int = 20,
    ) {
        assertEquals(expectedOffset, offset)
        assertEquals(expectedLimit, limit)
        assertEquals(expectedTotalElements, totalElements)
        assertTrue(now().isAfter(lastRefresh))
    }
}
