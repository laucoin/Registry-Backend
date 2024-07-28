package com.laucoin.registry.core.adapter

import com.laucoin.registry.core.model.util.ErrorEnum.NOT_IMPLEMENTED_YET
import com.laucoin.registry.core.model.util.RegistryExceptionModel
import java.io.Serializable
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication

class RegistryPermissionEvaluator: PermissionEvaluator {
    private val eventAuthorityRegex = Regex("""(.*_)(RO|RW|RWD)""")
    private val readOnly: String = "RO"
    private val readWrite: String = "RW"
    private val readWriteDelete: String = "RWD"

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
        throw RegistryExceptionModel(
            status = INTERNAL_SERVER_ERROR,
            errorCode = NOT_IMPLEMENTED_YET.name,
        )
    }

    private fun hasPrivilege(authentication: Authentication?, targetId: String?, permission: Any?): Boolean {
        if (Objects.isNull(authentication) || targetId.isNullOrBlank() || permission !is String) {
            return false
        }

        val acceptableAuthorities = getMatchingAuthorities(permission, targetId)
        return authentication !!.authorities.any { acceptableAuthorities.contains(it.authority) }
    }

    private fun getMatchingAuthorities(permission: String, targetId: String): List<String> =
        if (permission.startsWith("ROLE_REGISTRY_EVENT")) getEventMatchingAuthorities(permission, targetId)
        else listOf("${permission}_$targetId")

    private fun getEventMatchingAuthorities(permission: String, targetId: String): List<String> {
        val matchResult = eventAuthorityRegex.matchEntire(permission)

        when (matchResult?.groups?.last()?.value) {
            readOnly -> return listOf(
                "${matchResult.groups[1] !!.value}${readOnly}_$targetId",
                "${matchResult.groups[1] !!.value}${readWrite}_$targetId",
                "${matchResult.groups[1] !!.value}${readWriteDelete}_$targetId",
            )

            readWrite -> return listOf(
                "${matchResult.groups[1] !!.value}${readWrite}_$targetId",
                "${matchResult.groups[1] !!.value}${readWriteDelete}_$targetId",
            )

            readWriteDelete -> return listOf("${matchResult.groups[1] !!.value}${readWriteDelete}_$targetId")

            else -> return emptyList()
        }
    }
}
