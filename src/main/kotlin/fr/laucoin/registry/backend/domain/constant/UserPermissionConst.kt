package fr.laucoin.registry.backend.domain.constant

/**
 * Purpose: Declares the platform-scoped permission names.
 * Scope: Names matching the seeded `tb_user_permission` rows and the `@PreAuthorize` expressions.
 * Limits: Holds names only; granting is data in the role tables.
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
