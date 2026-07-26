package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto

open class GroupWithoutMemberReaderDto(
	var name: String? = null,
	var status: LabelDto? = null,
	var startAvailability: CustomDateTimeModel? = null,
	var endAvailability: CustomDateTimeModel? = null,
	var membersCount: Long? = null,
	var insideMembersCount: Long? = null,
	var outsideMembersCount: Long? = null,
) : GenericProjectReaderDto()
