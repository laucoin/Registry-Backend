package fr.laucoin.registry.backend.test

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application.yml"])
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class TestContext
