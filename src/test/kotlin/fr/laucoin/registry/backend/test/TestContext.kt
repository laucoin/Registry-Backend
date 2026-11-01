package fr.laucoin.registry.backend.test

import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
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
	 * Spring Boot 4 a supprimé l'auto-configuration qui appliquait [springSecurity] au mock
	 * [org.springframework.test.web.reactive.server.WebTestClient]. Sans elle, `mutateWith(mockUser(...))`
	 * n'a aucun effet et les endpoints sécurisés répondent 401. Enregistrer le bean [MockServerConfigurer] la rétablit,
	 * puisque `WebTestClientAutoConfiguration` applique encore chaque bean de ce type lors du rattachement au contexte applicatif.
	 */
	@TestConfiguration
	class WebTestClientSecurityConfiguration {
		@Bean
		fun springSecurityMockServerConfigurer(): MockServerConfigurer = springSecurity()

		/**
		 * `.authenticate()` (mockUser) contourne entièrement la couche bearer-token, donc les tests ne portent jamais
		 * de vrai token d'accès — CsrfTokenService.computeToken le résout de façon déterministe en
		 * `HMAC(secret, "")`, la même valeur que StatelessCsrfTokenRepository.loadToken calcule pour toute
		 * requête de ce type. La précalculer ici une fois et l'utiliser par défaut dans chaque appel webClient permet de
		 * faire passer toute la suite sans que chaque test ait à connaître le CSRF.
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
