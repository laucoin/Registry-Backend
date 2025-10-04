package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import java.util.Optional
import kotlin.time.Duration
import org.springframework.stereotype.Component

@Component
class ActivityEntityMapper: IEntityMapper<ActivityModel, ActivityEntity> {
	override fun toModel(entity: ActivityEntity): ActivityModel {
		return ActivityModel().apply {
			name = entity.name
			description = entity.description
			duration = mapDuration(entity.duration)
			allowedParticipants = mapAllowedParticipants(entity)
			startAvailability = mapCustomDateTime(entity.startAvailabilityDate, entity.startAvailabilityTime)
			endAvailability = mapCustomDateTime(entity.endAvailabilityDate, entity.endAvailabilityTime)
			status = buildStatus()
		}.fillWithProjectAndEntity(entity)
	}

	private fun mapDuration(duration: String?): Duration? {
		return Optional.ofNullable(duration).map(Duration::parse).orElse(null)
	}

	private fun mapAllowedParticipants(entity: ActivityEntity): NumericRangeModel? {
		return if (Objects.nonNull(entity.minAllowedParticipants) || Objects.nonNull(entity.maxAllowedParticipants))
			NumericRangeModel(entity.minAllowedParticipants, entity.maxAllowedParticipants)
		else null
	}

	override fun toEntity(model: ActivityModel): ActivityEntity {
		return ActivityEntity().apply {
			name = model.name
			description = model.description
			duration = Optional.ofNullable(model.duration).map(Duration::toString).orElse(null)
			minAllowedParticipants = model.allowedParticipants?.lower
			maxAllowedParticipants = model.allowedParticipants?.upper
			startAvailabilityDate = model.startAvailability?.date
			startAvailabilityTime = model.startAvailability?.time
			endAvailabilityDate = model.endAvailability?.date
			endAvailabilityTime = model.endAvailability?.time
		}.fillWithProjectAndModel(model)
	}
}
