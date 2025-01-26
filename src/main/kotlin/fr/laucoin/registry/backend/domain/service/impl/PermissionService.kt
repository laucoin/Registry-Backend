package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_IMPLEMENTED_YET
import fr.laucoin.registry.backend.domain.model.RegistryException
import java.io.Serializable
import java.util.Objects
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication

class PermissionService: PermissionEvaluator {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun hasPermission(authentication: Authentication?, targetObject: Any?, permission: Any?): Boolean {
        val idAsString = when (targetObject) {
            is UUID -> targetObject.toString()
            is String -> targetObject
            else -> return false
        }

        return hasPrivilege(authentication, idAsString, permission)
    }

    override fun hasPermission(
        authentication: Authentication?,
        targetId: Serializable?,
        targetType: String?,
        permission: Any?
    ): Boolean {
        val exception = RegistryException(
            status = NOT_IMPLEMENTED,
            code = NOT_IMPLEMENTED_YET,
        )
        log.error(
            "Prefer to use `hasPermission(authentication: Authentication?, targetObject: Any?, permission: Any?)`",
            exception,
        )
        throw exception
    }

    private fun hasPrivilege(authentication: Authentication?, targetId: String, permission: Any?): Boolean {
        if (Objects.isNull(authentication) || targetId.isBlank() || permission !is String) {
            return false
        }

        return authentication !!.authorities.any { it.authority == "${targetId}_$permission" }
    }
}
