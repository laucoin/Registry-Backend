package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel

open class GroupWithoutMemberReaderDto(
    var name: String? = null,
    var startAvailability: CustomDateTimeModel? = null,
    var endAvailability: CustomDateTimeModel? = null,
): GenericProjectReaderDto()
