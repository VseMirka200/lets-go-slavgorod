package ru.slavgorod.transport.data.model

import timber.log.Timber

internal data class ValidationCheck(
    val isValid: Boolean,
    val failureMessage: String
)

internal fun validateModel(
    exceptionMessage: String,
    vararg checks: ValidationCheck
): Boolean {
    return try {
        val failedChecks = checks.filterNot { it.isValid }
        if (failedChecks.isNotEmpty()) {
            failedChecks.forEach { check -> Timber.e(check.failureMessage) }
            return false
        }

        true
    } catch (exception: Exception) {
        Timber.e(exception, exceptionMessage)
        false
    }
}
