package io.github.luposolitario.immundanoctisex.core.data.validation

// errors: bloccano il caricamento del pacchetto. warnings: caricamento
// consentito, segnalazione soltanto (es. globalRule verso scena non-ENDING).
data class ValidationResult(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    operator fun plus(other: ValidationResult) = ValidationResult(
        errors = errors + other.errors,
        warnings = warnings + other.warnings,
    )

    companion object {
        val EMPTY = ValidationResult()
    }
}
