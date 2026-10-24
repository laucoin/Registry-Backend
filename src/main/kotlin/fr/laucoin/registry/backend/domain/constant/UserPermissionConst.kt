package fr.laucoin.registry.backend.domain.constant

/**
 * Objectif : Déclare les noms des permissions portées par la plateforme.
 * Périmètre : Noms alignés sur les lignes injectées de `tb_user_permission` et sur les expressions `@PreAuthorize`.
 * Limites : Ne porte que des noms ; l'attribution est une donnée des tables de rôles.
 */
object UserPermissionConst {
	const val REGISTRY_USER_R = "REGISTRY_USER_R"
	const val REGISTRY_USER_METADATA_R = "REGISTRY_USER_METADATA_R"
	const val REGISTRY_USER_U = "REGISTRY_USER_U"
	const val REGISTRY_USER_D = "REGISTRY_USER_D"
	const val REGISTRY_PROJECT_C = "REGISTRY_PROJECT_C"
	const val REGISTRY_PROJECT_R = "REGISTRY_PROJECT_R"
	const val REGISTRY_PROFILE_C = "REGISTRY_PROFILE_C"
	const val REGISTRY_JOB_C = "REGISTRY_JOB_C"
}
