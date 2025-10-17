package com.example.lets_go_slavgorod.utils

/**
 * Центральное хранилище констант приложения
 * 
 * Содержит все константы, используемые в приложении, организованные
 * по категориям для удобства поиска и обслуживания.
 * 
 * Категории констант:
 * - Информация о приложении (версия)
 * - Анимации (длительности)
 * - Размеры UI (отступы, размеры элементов)
 * - База данных (имя, версия)
 * - Цвета (маршруты, UI элементы)
 * - Уведомления (каналы, префиксы)
 * - Настройки (ключи DataStore)
 * 
 * Все значения должны обновляться только здесь для централизованного управления.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
object Constants {
    
    // Информация о приложении
    const val APP_VERSION = "v1.8"  // Версия приложения
    
    // Анимации (в миллисекундах)
    const val ANIMATION_DURATION_SHORT = 100  // Быстрые анимации (кнопки, переключатели)
    const val ANIMATION_DURATION_MEDIUM = 300 // Стандартные анимации (переходы между экранами)

    // Размеры UI (в dp)
    const val CARD_ELEVATION = 2                    // Тень карточек
    const val CARD_CORNER_RADIUS = 12              // Скругление углов карточек
    const val ROUTE_NUMBER_BOX_SIZE = 52           // Размер блока с номером маршрута
    const val ROUTE_NUMBER_BOX_CORNER_RADIUS = 16  // Скругление углов блока номера маршрута
    
    // Размеры карточек маршрутов
    const val ROUTE_CARD_HEIGHT_GRID = 180         // Высота карточки в режиме сетки
    const val ROUTE_NUMBER_BOX_SIZE_GRID = 75      // Размер блока номера в режиме сетки
    const val ROUTE_NUMBER_BOX_CORNER_RADIUS_GRID = 15  // Скругление блока номера в режиме сетки
    const val ROUTE_CARD_PADDING_GRID = 20         // Внутренние отступы карточки в режиме сетки
    
    // Размеры карточек расписания
    const val SCHEDULE_CARD_ELEVATION_DEFAULT = 1  // Стандартная тень карточки расписания
    const val SCHEDULE_CARD_ELEVATION_UPCOMING = 3 // Увеличенная тень для следующего рейса
    const val SCHEDULE_CARD_CORNER_RADIUS = 8      // Скругление углов карточки расписания
    const val SCHEDULE_CARD_PADDING = 12           // Внутренний отступ карточки расписания
    
    // Размеры элементов расписания
    const val FILTER_CHIP_HEIGHT = 48              // Высота кнопок фильтров
    const val FILTER_ICON_SIZE = 24                // Размер иконок в фильтрах
    const val FAVORITE_ICON_SIZE = 20              // Размер иконки избранного в компактной карточке
    const val FAVORITE_BUTTON_SIZE = 32            // Размер кнопки избранного
    // Отступы (в dp)
    const val PADDING_SMALL = 8   // Малые отступы (между элементами)
    const val PADDING_MEDIUM = 16 // Средние отступы (между секциями)
    const val PADDING_LARGE = 24  // Большие отступы (от краев экрана)
    const val PADDING_FILTER_TOP = 4   // Отступ сверху для фильтров
    const val PADDING_FILTER_BOTTOM = 2 // Отступ снизу для фильтров
    
    // Цвета по умолчанию (в формате ARGB)
    const val DEFAULT_ROUTE_COLOR = "#FF6200EE"    // Основной цвет маршрутов (фиолетовый)
    const val DEFAULT_ROUTE_COLOR_ALT = "#FF1976D2" // Альтернативный цвет маршрутов (синий)
    const val DEFAULT_ROUTE_COLOR_GREEN = "#FF4CAF50" // Зеленый цвет маршрутов
    const val COLOR_ALPHA = 0.9f                   // Прозрачность цветов (90%)
    
    // Уведомления
    const val NOTIFICATION_LEAD_TIME_MINUTES = 5  // За сколько минут до отправления показывать уведомление
    const val ALARM_REQUEST_CODE_PREFIX = "fav_alarm_" // Префикс для кодов будильников избранных маршрутов
    
    // Удалённая загрузка данных
    // Настройка GitHub репозитория
    const val GITHUB_USERNAME = "VseMirka200"                    // Ваш GitHub username
    const val GITHUB_REPO = "lets-go-slavgorod"                  // Название репозитория
    const val GITHUB_BRANCH = "main"                             // Ветка для загрузки (main, develop и т.д.)
    const val GITHUB_FILE_PATH = "materials/schedule/routes_data.json"  // Путь к файлу в репозитории
    
    // Автоматически формируемый URL (не изменяйте, измените параметры выше)
    const val REMOTE_JSON_URL = "https://raw.githubusercontent.com/$GITHUB_USERNAME/$GITHUB_REPO/$GITHUB_BRANCH/$GITHUB_FILE_PATH"
    
    const val REMOTE_CONNECTION_TIMEOUT = 10_000  // Таймаут подключения (мс)
    const val REMOTE_READ_TIMEOUT = 15_000        // Таймаут чтения (мс)
    // База данных
    const val DATABASE_NAME = "bus_app_database"  // Имя файла базы данных
    const val DATABASE_VERSION = 6               // Версия схемы базы данных
    
    // Поиск и производительность
    const val SEARCH_DEBOUNCE_DELAY = 300L       // Задержка перед поиском (мс)
    const val MAX_SEARCH_RESULTS = 100           // Максимальное количество результатов поиска
    
    
    // Кэширование
    const val CACHE_SIZE = 50                    // Максимальный размер кэша (элементов)
    const val CACHE_EXPIRE_TIME_HOURS = 24      // Время жизни кэша (часы)
    
    // Логирование (уровни важности)
    const val LOG_LEVEL_DEBUG = 0  // Отладочная информация
    const val LOG_LEVEL_INFO = 1   // Информационные сообщения
    const val LOG_LEVEL_WARN = 2   // Предупреждения
    const val LOG_LEVEL_ERROR = 3  // Ошибки
}
