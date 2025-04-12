package fr.laucoin.registry.backend.config

import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jose.shaded.gson.GsonBuilder
import com.nimbusds.jose.shaded.gson.TypeAdapter
import com.nimbusds.jose.shaded.gson.stream.JsonReader
import com.nimbusds.jose.shaded.gson.stream.JsonToken.NULL
import com.nimbusds.jose.shaded.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.Objects
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GsonConfig {
    @Bean
    fun gson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeTypeAdapter())
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .registerTypeAdapter(LocalTime::class.java, LocalTimeTypeAdapter())
            .create()
    }

    class ZonedDateTimeTypeAdapter: TypeAdapter<ZonedDateTime>() {
        private val formatter = ISO_ZONED_DATE_TIME

        override fun read(`in`: JsonReader): ZonedDateTime? {
            if (`in`.peek() == NULL) {
                `in`.nextNull()
                return null
            }

            return ZonedDateTime.parse(`in`.nextString(), formatter)
        }

        override fun write(out: JsonWriter, value: ZonedDateTime?) {
            if (Objects.isNull(value)) out.nullValue()
            else out.value(formatter.format(value))
        }
    }

    class LocalDateTypeAdapter: TypeAdapter<LocalDate>() {
        private val formatter = ISO_LOCAL_DATE

        override fun read(`in`: JsonReader): LocalDate? {
            if (`in`.peek() == NULL) {
                `in`.nextNull()
                return null
            }

            return LocalDate.parse(`in`.nextString(), formatter)
        }

        override fun write(out: JsonWriter, value: LocalDate?) {
            if (Objects.isNull(value)) out.nullValue()
            else out.value(formatter.format(value))
        }
    }

    class LocalTimeTypeAdapter: TypeAdapter<LocalTime>() {
        private val formatter = ISO_LOCAL_TIME

        override fun read(`in`: JsonReader): LocalTime? {
            if (`in`.peek() == NULL) {
                `in`.nextNull()
                return null
            }

            return LocalTime.parse(`in`.nextString(), formatter)
        }

        override fun write(out: JsonWriter, value: LocalTime?) {
            if (Objects.isNull(value)) out.nullValue()
            else out.value(formatter.format(value))
        }
    }
}
