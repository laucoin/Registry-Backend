package fr.laucoin.registry.backend.test

import fr.laucoin.registry.backend.domain.service.impl.CsrfTokenService
import fr.laucoin.registry.backend.domain.service.impl.CsrfTokenService.Companion.HEADER_NAME
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.webtestclient.autoconfigure.WebTestClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.MockServerConfigurer
import org.springframework.test.web.reactive.server.WebTestClient

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

		/**
		 * `.authenticate()` (mockUser) bypasses the bearer-token layer entirely, so tests never carry
		 * a real access token — CsrfTokenService.computeToken deterministically resolves that to
		 * `HMAC(secret, "")`, the same value StatelessCsrfTokenRepository.loadToken computes for any
		 * such request. Precomputing it once here and defaulting every webClient call to it keeps the
		 * whole suite passing without each test needing to know about CSRF at all.
		 */
		@Bean
		fun csrfHeaderWebTestClientBuilderCustomizer(csrfTokenService: CsrfTokenService): WebTestClientBuilderCustomizer {
			val noBearerExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
			val expectedToken = csrfTokenService.computeToken(noBearerExchange)
			return WebTestClientBuilderCustomizer { builder: WebTestClient.Builder ->
				builder.defaultHeader(HEADER_NAME, expectedToken)
			}
		}
	}
}
