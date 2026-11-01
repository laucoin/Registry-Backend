package fr.laucoin.registry.backend.domain.constant

/**
 * Objectif : Déclare les tailles maximales et jeux de caractères acceptés pour les entrées de requête, une constante par champ d'objet.
 * Périmètre : Porte les bornes appliquées par la bean validation sur les DTO d'écriture, les corps de requête et les paramètres de requête.
 * Limites : Ne porte aucune valeur propre à un environnement, qui reste dans les propriétés de configuration typées.
 */
object ValidationLimitsConst {
	const val PAGE_DEFAULT = "0"
	const val PAGE_MIN = 0L
	const val PAGE_MAX = 1_000_000L
	const val PAGE_SIZE_DEFAULT = "20"
	const val PAGE_SIZE_MIN = 1L
	const val PAGE_SIZE_MAX = 200L

	const val SEARCH_QUERY_MAX = 100
	const val SORT_MAX = 32

	const val ACTIVITY_NAME_MAX = 150
	const val ACTIVITY_DESCRIPTION_MAX = 2000
	const val ACTIVITY_DURATION_MAX = 32

	const val ALERT_TITLE_MAX = 50
	const val ALERT_MESSAGE_MAX = 250

	const val AUTHENTICATION_REDIRECT_URI_MAX = 2048
	const val AUTHENTICATION_STATE_MAX = 256
	const val AUTHENTICATION_AUTHORIZATION_CODE_MAX = 512

	const val COMMUNICATION_MESSAGE_MAX = 250

	const val GROUP_NAME_MAX = 150
	const val GROUP_MEMBER_IDS_MAX = 200

	const val GUEST_FIRST_NAME_MAX = 150
	const val GUEST_LAST_NAME_MAX = 150
	const val GUEST_MOVEMENT_CONTENT_MAX = 200
	const val GUEST_MOVEMENT_GUESTS_MAX = 200

	const val MOVEMENT_IDS_MAX = 80
	const val MOVEMENT_POOL_NAME_MAX = 150
	const val PARTICIPANT_MOVEMENT_CONTENT_MAX = 200

	const val PARTICIPANT_FIRST_NAME_MAX = 150
	const val PARTICIPANT_LAST_NAME_MAX = 150
	const val PARTICIPANT_GROUP_IDS_MAX = 200

	const val PURGE_REPORTED_IDS_MAX = 1000

	const val PREFERENCES_LANGUAGE_MAX = 10

	const val PROJECT_NAME_MAX = 150
	const val PROJECT_OPTIONS_MAX = 20

	const val PROJECT_PROFILE_ROLE_MAX = 50
	const val PROJECT_PROFILE_USER_IDS_MAX = 200

	const val USER_ROLE_MAX = 50

	const val VEHICLE_LICENSE_PLATE_MAX = 20
	const val VEHICLE_BRAND_MAX = 150
	const val VEHICLE_MODEL_MAX = 150

	const val STATE_PATTERN = "[A-Za-z0-9._~\\-]*"
	const val ROLE_PATTERN = "[A-Za-z0-9_]*"
	const val DURATION_PATTERN = "[A-Za-z0-9.,:+\\- ]*"
}
