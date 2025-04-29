package fr.laucoin.registry.backend.test

import fr.laucoin.registry.backend.domain.model.PageModel
import java.time.ZonedDateTime.now
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

object ModelExt {

    val projectId: UUID = UUID.fromString("b7432b97-cfc6-4109-aaaa-38d348523f1e")
    val projectProfileId: UUID = UUID.fromString("28d92461-addb-42d5-9301-18ef6e966608")
    val userIdWithoutProfile: UUID = UUID.fromString("e22a08da-b8b8-4b78-86c8-8557ddfbb945")
    val groupId: UUID = UUID.fromString("acb4943c-a911-4f1d-b899-69f6cfcfef90")
    val movementId: UUID = UUID.fromString("63f4c4e8-bd07-445b-8a6e-899ac490cf0c")
    val participantId: UUID = UUID.fromString("88f7194e-6633-4f84-b3e3-8546b51d07e0")
    val activityId: UUID = UUID.fromString("95806471-9c01-477a-84ea-8c37fd0cc8c5")
    val vehicleId: UUID = UUID.fromString("7ae25102-8337-4836-93e5-dd2cd8c5d5ec")

    fun <T> PageModel<T>.assertPage(
        expectedTotalElements: Int,
        expectedOffset: Int = 0,
        expectedLimit: Int = 20,
    ) {
        assertEquals(expectedOffset, pageNumber)
        assertEquals(expectedLimit, pageSize)
        assertEquals(expectedTotalElements, totalElements)
        assertTrue(now().isAfter(lastRefresh))
    }
}
