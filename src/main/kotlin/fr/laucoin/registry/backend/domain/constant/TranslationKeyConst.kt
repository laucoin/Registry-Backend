package fr.laucoin.registry.backend.domain.constant

/**
 * Purpose: Declares the prefixes of the translation keys.
 * Scope: Groups the message bundle key prefixes used to label enums and errors.
 * Limits: Holds prefixes only, never message text.
 */
object TranslationKeyConst {
	const val ALERT_CRITICALITY_PREFIX = "alert.criticality."
	const val ALERT_CRITICALITY_RULE_KEY = "alert.criticality.rule"
	const val ALERT_CRITICALITY_RULE_PREFIX = "$ALERT_CRITICALITY_RULE_KEY."
	const val ALERT_STATUS_PREFIX = "alert.status."
	const val DURATION_PREFIX = "duration."
	const val ERROR_TITLE_PREFIX = "error.title."
	const val ERROR_MESSAGE_PREFIX = "error.message."
	const val USER_ROLE_PREFIX = "user.role."
	const val MOVEMENT_REASON_PREFIX = "movement.reason."
	const val MOVEMENT_TYPE_PREFIX = "movement.type."
	const val PARTICIPANT_TYPE_PREFIX = "participant.type."
	const val PRESENCE_STATUS_PREFIX = "presence.status."
	const val PRESENCE_STATUS_DURATION_PREFIX = "$PRESENCE_STATUS_PREFIX$DURATION_PREFIX"
	const val AVAILABILITY_STATUS_PREFIX = "availability.status."
	const val PROJECT_OPTION_FORM_ASK_PREFIX = "project.option.form-ask."
	const val PROJECT_OPTION_NAME_PREFIX = "project.option.name."
	const val PROJECT_PROFILE_ROLE_PREFIX = "project-profile.role."
	const val PROJECT_PROFILE_STATUS_PREFIX = "project-profile.status."
}
