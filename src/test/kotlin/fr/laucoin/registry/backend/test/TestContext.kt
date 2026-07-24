package fr.laucoin.registry.backend.test

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.MockServerConfigurer

@SpringBootTest(webEnvironment = MOCK)
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application.yml"])
@AutoConfigureWebTestClient
@Import(TestContext.WebTestClientSecurityConfiguration::class)
@ContextConfiguration(initializers = [TestContainerDatabase::class])
class TestContext {

	/**
	 * Spring Boot 4 dropped the auto-configuration that applied [springSecurity] to the mock
	 * [org.springframework.test.web.reactive.server.WebTestClient]. Without it, `mutateWith(mockUser(...))`
	 * has no effect and secured endpoints answer 401. Registering the [MockServerConfigurer] bean restores it,
	 * since `WebTestClientAutoConfiguration` still applies every such bean when binding to the application context.
	 */
	@TestConfiguration
	class WebTestClientSecurityConfiguration {
		@Bean
		fun springSecurityMockServerConfigurer(): MockServerConfigurer = springSecurity()
	}
}
