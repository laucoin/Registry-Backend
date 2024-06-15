package com.laucoin.registry.core.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.Objects

class LocalDateTimeTypeAdapter: TypeAdapter<LocalDateTime>() {
    private val formatter = ISO_LOCAL_DATE_TIME

    override fun read(`in`: JsonReader): LocalDateTime? {
        var value: String? = null
        if (`in`.hasNext()) {
            value = `in`.nextString()
        }

        if (value.isNullOrBlank()) return null

        return LocalDateTime.parse(value, formatter)
    }

    override fun write(out: JsonWriter, value: LocalDateTime?) {
        if (Objects.isNull(value)) out.nullValue()
        else out.value(formatter.format(value))
    }
}

