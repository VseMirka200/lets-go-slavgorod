package com.example.lets_go_slavgorod.updates

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.lets_go_slavgorod.updates.UpdateManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit-тесты для UpdateManager
 * 
 * Проверяет корректность работы менеджера обновлений:
 * - Проверка версий
 * - Сравнение версий
 * - Обработка ошибок сети
 * - Валидация данных
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
@ExperimentalCoroutinesApi
class UpdateManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var updateManager: UpdateManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        updateManager = UpdateManager(mockContext)
    }
    
    @Test
    fun `compareVersions returns true when new version is greater`() {
        // When
        val result = updateManager.compareVersions("2.0", "1.0")
        
        // Then
        assertTrue(result, "2.0 should be greater than 1.0")
    }
    
    @Test
    fun `compareVersions returns false when versions are equal`() {
        // When
        val result = updateManager.compareVersions("1.0", "1.0")
        
        // Then
        assertFalse(result, "1.0 should not be greater than 1.0")
    }
    
    @Test
    fun `compareVersions returns false when new version is lower`() {
        // When
        val result = updateManager.compareVersions("1.0", "2.0")
        
        // Then
        assertFalse(result, "1.0 should not be greater than 2.0")
    }
    
    @Test
    fun `compareVersions handles multi-digit versions`() {
        // When
        val result1 = updateManager.compareVersions("1.10", "1.9")
        val result2 = updateManager.compareVersions("1.9", "1.10")
        
        // Then
        assertTrue(result1, "1.10 should be greater than 1.9")
        assertFalse(result2, "1.9 should not be greater than 1.10")
    }
    
    @Test
    fun `compareVersions handles three-part versions`() {
        // When
        val result1 = updateManager.compareVersions("1.2.3", "1.2.2")
        val result2 = updateManager.compareVersions("1.2.2", "1.2.3")
        
        // Then
        assertTrue(result1, "1.2.3 should be greater than 1.2.2")
        assertFalse(result2, "1.2.2 should not be greater than 1.2.3")
    }
    
    @Test
    fun `compareVersions handles different length versions`() {
        // When
        val result1 = updateManager.compareVersions("1.2.1", "1.2")
        val result2 = updateManager.compareVersions("1.2", "1.2.1")
        
        // Then
        assertTrue(result1, "1.2.1 should be greater than 1.2")
        assertFalse(result2, "1.2 should not be greater than 1.2.1")
    }
    
    @Test
    fun `compareVersions handles invalid versions gracefully`() {
        // When/Then - не должно крашиться
        val result1 = updateManager.compareVersions("invalid", "1.0")
        val result2 = updateManager.compareVersions("1.0", "invalid")
        val result3 = updateManager.compareVersions("", "1.0")
        
        // Should handle gracefully without crashing
        assertFalse(result1)
        assertFalse(result2)
        assertFalse(result3)
    }
    
    @Test
    fun `getCurrentVersion returns valid version string`() {
        // When
        val version = updateManager.getCurrentVersion()
        
        // Then
        assertNotNull(version, "Current version should not be null")
        assertTrue(version.isNotBlank(), "Current version should not be blank")
    }
    
    @Test
    fun `UpdateResult data class works correctly`() {
        // Given
        val updateInfo = UpdateManager.UpdateInfo(
            versionName = "2.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Bug fixes"
        )
        
        // When
        val successResult = UpdateManager.UpdateResult(
            success = true,
            update = updateInfo,
            error = null
        )
        
        val errorResult = UpdateManager.UpdateResult(
            success = false,
            update = null,
            error = "Network error"
        )
        
        // Then
        assertTrue(successResult.success)
        assertNotNull(successResult.update)
        assertEquals("2.0", successResult.update?.versionName)
        
        assertFalse(errorResult.success)
        assertNull(errorResult.update)
        assertNotNull(errorResult.error)
    }
}

