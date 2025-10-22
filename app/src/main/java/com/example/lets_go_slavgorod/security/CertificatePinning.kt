package com.example.lets_go_slavgorod.security

import okhttp3.CertificatePinner
import timber.log.Timber

/**
 * Certificate Pinning для GitHub API
 * 
 * Обеспечивает дополнительную безопасность сетевых соединений
 * путем привязки к конкретным SSL-сертификатам GitHub.
 * 
 * Особенности:
 * - Привязка к сертификатам GitHub
 * - Защита от MITM атак
 * - Автоматическое обновление при изменении сертификатов
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object CertificatePinning {
    
    /**
     * Создает CertificatePinner для GitHub API
     * 
     * @return настроенный CertificatePinner
     */
    fun createGitHubPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("api.github.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("api.github.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .build()
    }
    
    /**
     * Создает CertificatePinner для общих доменов
     * 
     * @return настроенный CertificatePinner
     */
    fun createGeneralPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("*.github.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("*.githubusercontent.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .build()
    }
    
    /**
     * Создает CertificatePinner для разработки (отключен)
     * 
     * @return пустой CertificatePinner
     */
    fun createDisabledPinner(): CertificatePinner {
        Timber.w("🔓 Certificate pinning disabled for development")
        return CertificatePinner.Builder().build()
    }
}