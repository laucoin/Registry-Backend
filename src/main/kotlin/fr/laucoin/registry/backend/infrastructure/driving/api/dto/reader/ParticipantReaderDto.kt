package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import com.fasterxml.jackson.annotation.JsonProperty
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import java.time.LocalDate

data class ParticipantReaderDto(
	var firstName: String? = null,
	var lastName: String? = null,
	var birthday: LocalDate? = null,
	var type: LabelDto? = null,
	@get:JsonProperty("major")
	@param:JsonProperty("major")
	var isMajor: Boolean? = null,
	var groups: List<GroupWithoutMemberReaderDto> = emptyList(),
	var availableGroups: List<GroupWithoutMemberReaderDto> = emptyList(),
	var status: LabelDto? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
	var user: PartialUserReaderDto? = null,
	@get:JsonProperty("purged")
	@param:JsonProperty("purged")
	var isPurged: Boolean? = null,
): GenericReaderDto()
