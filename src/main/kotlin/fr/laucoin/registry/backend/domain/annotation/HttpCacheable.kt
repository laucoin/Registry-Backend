package fr.laucoin.registry.backend.domain.annotation

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * ADR 018 §4 — marks a code-derived reference GET (metadata) whose response
 * carries the HTTP revalidation headers (ETag / Cache-Control / Vary) and can
 * short-circuit to `304 Not Modified`. Declared on the controller contract
 * method so the behaviour is visible where the endpoint is defined;
 * [fr.laucoin.registry.backend.domain.handler.CacheHeadersHandler] applies it.
 *
 * The `304` short-circuit answers before the controller method is invoked, so
 * method security would be silently skipped: never combine this annotation
 * with `@PreAuthorize` — cacheable endpoints must stay authentication-only
 * (an architecture test enforces the constraint).
 */
@Target(FUNCTION)
@Retention(RUNTIME)
annotation class HttpCacheable
