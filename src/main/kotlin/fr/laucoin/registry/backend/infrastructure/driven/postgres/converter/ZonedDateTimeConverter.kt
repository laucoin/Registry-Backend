package fr.laucoin.registry.backend.infrastructure.driven.postgres.converter

import org.jooq.Converter
import java.time.OffsetDateTime
import java.time.ZonedDateTime

// Binds every TIMESTAMP WITH TIME ZONE column to ZonedDateTime (every entity's type); wired via forcedTypes in build.gradle.kts.
class ZonedDateTimeConverter : Converter<OffsetDateTime, ZonedDateTime> {
	override fun from(databaseObject: OffsetDateTime?): ZonedDateTime? = databaseObject?.toZonedDateTime()

	override fun to(userObject: ZonedDateTime?): OffsetDateTime? = userObject?.toOffsetDateTime()

	override fun fromType(): Class<OffsetDateTime> = OffsetDateTime::class.java

	override fun toType(): Class<ZonedDateTime> = ZonedDateTime::class.java
}
