package fr.laucoin.registry.backend.domain.enumeration

enum class ProjectOptionEnum : IProjectOptionEnum {
	VEHICLE {
		override val requiredOptions = emptyList<ProjectOptionEnum>()
	},
	ACTIVITY {
		override val requiredOptions = emptyList<ProjectOptionEnum>()
	},
	COMMUNICATION {
		override val requiredOptions = listOf(ACTIVITY)
	},
	ALERT {
		override val requiredOptions = listOf(ACTIVITY, COMMUNICATION)
	};
}
