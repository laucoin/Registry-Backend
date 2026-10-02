package fr.laucoin.registry.backend.infrastructure.driven.postgres.converter

import org.jooq.Converter
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * jOOQ [Converter] mapping a Postgres `timestamptz` (`OffsetDateTime`) column to/from [ZonedDateTime],
 * so generated entity fields can be typed with the zone-aware type the domain uses.
 */
class ZonedDateTimeConverter : Converter<OffsetDateTime, ZonedDateTime> {
	override fun from(databaseObject: OffsetDateTime?): ZonedDateTime? = databaseObject?.toZonedDateTime()

	override fun to(userObject: ZonedDateTime?): OffsetDateTime? = userObject?.toOffsetDateTime()

	override fun fromType(): Class<OffsetDateTime> = OffsetDateTime::class.java

	override fun toType(): Class<ZonedDateTime> = ZonedDateTime::class.java
}
