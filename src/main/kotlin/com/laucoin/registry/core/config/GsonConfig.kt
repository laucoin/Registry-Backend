package com.laucoin.registry.core.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.laucoin.registry.core.adapter.LocalDateTimeTypeAdapter
import com.laucoin.registry.core.adapter.LocalDateTypeAdapter
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GsonConfig {
    @Bean
    fun gson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
            .create()
    }
}
