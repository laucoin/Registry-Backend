package com.laucoin.registry.core.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.Objects

class LocalDateTypeAdapter: TypeAdapter<LocalDate>() {
    private val formatter = ISO_LOCAL_DATE

    override fun read(`in`: JsonReader): LocalDate? {
        var value: String? = null
        if (`in`.hasNext()) {
            value = `in`.nextString()
        }

        if (value.isNullOrBlank()) return null

        return LocalDate.parse(value, formatter)
    }

    override fun write(out: JsonWriter, value: LocalDate?) {
        if (Objects.isNull(value)) out.nullValue()
        else out.value(formatter.format(value))
    }
}
