package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jose.shaded.gson.reflect.TypeToken
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementEntity
import java.time.ZonedDateTime
import org.springframework.stereotype.Component

@Component
class MovementEntityMapper(private val gson: Gson): IGenericEventEntityMapper<MovementModel, MovementEntity> {
    private val listType = object: TypeToken<List<MovementContentModel>>() {}.type

    override fun toModel(entity: MovementEntity): MovementModel {
        return MovementModel().apply {
            dateTime = entity.dateTime ?: ZonedDateTime.now()
            type = entity.type
            content = gson.fromJson(entity.content, listType)
            event = mapEventEntity(entity)
        }.fillWithEntity(entity)
    }

    override fun toEntity(model: MovementModel): MovementEntity {
        return MovementEntity().apply {
            dateTime = model.dateTime
            type = model.type
            eventId = model.event?.id
        }.fillWithModel(model)
    }
}
