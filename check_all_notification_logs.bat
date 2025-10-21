@echo off
chcp 65001 >nul
echo ╔════════════════════════════════════════════════════════════╗
echo ║   Просмотр ВСЕХ логов связанных с уведомлениями            ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
echo Подключение к устройству...
echo.

REM Проверяем подключение устройства
adb devices

echo.
echo ════════════════════════════════════════════════════════════
echo   Комплексные логи системы уведомлений
echo ════════════════════════════════════════════════════════════
echo.
echo Показывает:
echo   1. Вычисление следующего уведомления (NotificationTimeCalculator)
echo   2. Отображение таймера (NextNotificationTimer)
echo   3. Срабатывание будильника (AlarmReceiver)
echo   4. Проверку настроек (NotificationPreferencesCache)
echo.
echo Инструкция:
echo   - Добавьте время в избранное (например 7:00)
echo   - Логи покажут ВСЕ шаги вычисления
echo   - Вы увидите почему показывает "через X дней"
echo   - Ctrl+C чтобы остановить
echo.
echo ════════════════════════════════════════════════════════════
echo.

REM Показываем логи всех компонентов
adb logcat -v time *:S ^
  NotificationTimeCalculator:D ^
  NextNotificationTimer:D ^
  AlarmReceiver:D ^
  NotificationPreferencesCache:D ^
  AlarmScheduler:D ^
  BusViewModel:D

pause

