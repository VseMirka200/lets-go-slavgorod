@echo off
REM ========================================
REM Проверка логов времени уведомления
REM ========================================

echo ================================================
echo Проверка настроек времени уведомления
echo ================================================
echo.

echo [1] Логи изменения времени уведомления:
adb logcat -d | findstr "lead time"
echo.
echo ================================================
echo.

echo [2] Логи планирования уведомлений:
adb logcat -d | findstr "Lead time for route"
echo.
echo ================================================
echo.

echo [3] Логи срабатывания уведомлений (AlarmReceiver):
adb logcat -d | findstr "Alarm received"
echo.
echo ================================================
echo.

echo [4] Логи обновления уведомлений:
adb logcat -d | findstr "Updating all alarms"
echo.
echo ================================================
echo.

echo [5] Полные логи NotificationTimePreferences:
adb logcat -d -s NotificationTimePreferences:* AlarmScheduler:* AlarmReceiver:*
echo.
echo ================================================

pause

