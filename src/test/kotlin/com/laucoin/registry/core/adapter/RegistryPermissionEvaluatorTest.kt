package com.laucoin.registry.core.adapter

import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_ACTIVITY_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_ACTIVITY_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_ACTIVITY_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_MOVEMENT_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_MOVEMENT_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_MOVEMENT_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_PARTICIPANT_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_PARTICIPANT_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_PARTICIPANT_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_RWD
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_VEHICLE_RO
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_VEHICLE_RW
import com.laucoin.registry.core.model.event.EventAuthorityEnum.ROLE_REGISTRY_EVENT_VEHICLE_RWD
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority

class RegistryPermissionEvaluatorTest {
    private val registryPermissionEvaluator = RegistryPermissionEvaluator()

    companion object {
        val uuid = UUID.randomUUID()

        @JvmStatic
        fun `Should test hasPermission with multiple role`(): Stream<Arguments> = Stream.of(
            Arguments.of("ROLE_REGISTRY_USER_$uuid", uuid, "ROLE_REGISTRY_USER", true),

            Arguments.of("ROLE_REGISTRY_EVENT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_RWD.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_RW.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_RO.name, true),

            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RWD.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RW.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_ACTIVITY_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_ACTIVITY_RO.name, true),

            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RWD.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RW.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_VEHICLE_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_VEHICLE_RO.name, true),

            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RWD.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RW.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_PARTICIPANT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_PARTICIPANT_RO.name, true),

            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RWD.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RWD.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RW.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RW.name, false),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RWD_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RW_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RO.name, true),
            Arguments.of("ROLE_REGISTRY_EVENT_MOVEMENT_RO_$uuid", uuid, ROLE_REGISTRY_EVENT_MOVEMENT_RO.name, true),

            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_PHONE_COMMUNICATION_RO.name,
                true
            ),

            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_ACTIVITY_COMMUNICATION_RO.name,
                true
            ),

            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW.name,
                false
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RWD_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RW_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO.name,
                true
            ),
            Arguments.of(
                "ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO_$uuid",
                uuid,
                ROLE_REGISTRY_EVENT_DEFENSE_AGAINST_FIRE_RO.name,
                true
            ),

            Arguments.of("ROLE_REGISTRY_EVENT_RWD_$uuid", uuid, "ROLE_REGISTRY_EVENT_RWD_FAILED", false),
            Arguments.of("ROLE_REGISTRY_USER_$uuid", 42, "ROLE_REGISTRY_USER", false),
            Arguments.of("ROLE_REGISTRY_USER_$uuid", null, "ROLE_REGISTRY_USER", false),
            Arguments.of("ROLE_REGISTRY_USER_$uuid", "", "ROLE_REGISTRY_USER", false),
            Arguments.of("ROLE_REGISTRY_USER_$uuid", uuid, 42, false),
            Arguments.of("ROLE_REGISTRY_USER_$uuid", null, 42, false),
        )
    }

    @MethodSource
    @ParameterizedTest
    fun `Should test hasPermission with multiple role`(
        authority: String,
        targetId: Any?,
        permission: Any?,
        expectedValue: Boolean
    ) {
        // Arrange
        val authentication: Authentication = mock(Authentication::class.java)
        `when`(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority(authority)))

        // Act
        val result = registryPermissionEvaluator.hasPermission(authentication, targetId, permission)

        // Assert
        assertEquals(expectedValue, result)
    }

    @Test
    fun `Should hasPermission return false with null authentication`() {
        // Arrange
        val authentication: Authentication? = null

        // Act
        val result = registryPermissionEvaluator.hasPermission(authentication, uuid, "ROLE_REGISTRY_USER")

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `Should hasPermission throw a RegistryExceptionModel`() {
        // Arrange
        // Act, Assert
        val result = assertThrows(RegistryExceptionModel::class.java) {
            registryPermissionEvaluator.hasPermission(authentication = null, targetId = null, targetType = null, permission = null)
        }
        assertEquals(INTERNAL_SERVER_ERROR, result.statusCode)
        assertEquals(0, result.args.size)
        assertEquals("500 INTERNAL_SERVER_ERROR \"NOT_IMPLEMENTED_YET\"", result.message)
    }
}
