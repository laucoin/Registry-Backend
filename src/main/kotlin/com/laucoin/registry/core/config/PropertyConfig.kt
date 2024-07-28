package com.laucoin.registry.core.config

import com.laucoin.registry.core.adapter.SecurityProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class PropertyConfig
