package com.laucoin.registry.core.config

import com.laucoin.registry.core.adapter.AppManagementProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AppManagementProperties::class)
class PropertyConfig
