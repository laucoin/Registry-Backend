package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.LocalDate

data class ParticipantReaderDto(
    var firstName: String? = null,
    var lastName: String? = null,
    var birthday: LocalDate? = null,
    var type: LabelDto? = null,
    var major: Boolean? = null,
    var groups: List<GroupWithoutMemberReaderDto> = emptyList(),
    var availableGroups: List<GroupWithoutMemberReaderDto> = emptyList(),
    var status: LabelDto? = null,
    var startAvailability: CustomDateTimeModel? = null,
    var endAvailability: CustomDateTimeModel? = null,
    var user: PartialUserReaderDto? = null,
    var purged: Boolean? = null,
): GenericEventReaderDto()
