package fr.laucoin.registry.backend.test

/** Remplace les marqueurs SpEL `:#{#c.champ}` des gabarits SQL par des paramètres nommés `:champ`, pour les exécuter avec `DatabaseClient`. */
fun String.withNamedParameters(): String = replace(Regex(":#\\{#(?:c|page)\\.(\\w+)}"), ":$1")
