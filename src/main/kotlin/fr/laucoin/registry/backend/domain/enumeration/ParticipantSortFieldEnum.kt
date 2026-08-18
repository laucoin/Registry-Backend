package fr.laucoin.registry.backend.domain.enumeration

enum class ParticipantSortFieldEnum(val paramName: String) {
	FIRST_NAME("firstName"),
	LAST_NAME("lastName"),
	BIRTHDAY("birthday"),
	TYPE("type");

	companion object {
		fun fromParamName(paramName: String): ParticipantSortFieldEnum? =
			entries.firstOrNull { it.paramName == paramName }
	}
}
