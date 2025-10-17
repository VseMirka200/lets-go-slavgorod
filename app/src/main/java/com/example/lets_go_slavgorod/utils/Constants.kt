package com.example.lets_go_slavgorod.utils

/**
 * Центральное хранилище констант приложения
 * 
 * Версия: 2.1
 * Последнее обновление: Октябрь 2025
 * 
 * Содержит все константы, используемые в приложении, организованные
 * по категориям для удобства поиска и централизованного управления.
 * Все размеры, отступы и настройки должны обновляться только здесь.
 * 
 * Категории констант:
 * - **Информация о приложении** (версия, название)
 * - **Анимации** (длительности переходов и эффектов)
 * - **Размеры UI** (отступы, размеры элементов, скругления)
 * - **Размеры карточек** (маршрутов и расписаний)
 * - **База данных** (имя, версия схемы)
 * - **Цвета** (маршруты, UI элементы, прозрачность)
 * - **Уведомления** (таймауты, каналы, префиксы)
 * - **Удаленная загрузка** (GitHub API, таймауты)
 * - **Поиск и кэширование** (задержки, лимиты)
 * - **Логирование** (уровни важности)
 * 
 * Изменения v2.1:
 * - Добавлены константы для фильтров (FILTER_CHIP_HEIGHT, FILTER_ICON_SIZE)
 * - Добавлены константы для отступов фильтров (PADDING_FILTER_TOP/BOTTOM)
 * - Добавлены размеры иконок избранного (FAVORITE_ICON_SIZE, FAVORITE_BUTTON_SIZE)
 * - Унифицированы отступы между элементами расписания
 * 
 * Все значения должны обновляться только здесь для централизованного управления.
 * 
 * @author VseMirka200
 * @version 2.1
 * @since 1.0
 */
object Constants {
    
    // Информация о приложении
    const val APP_VERSION = "v2.0"  // Версия приложения
    
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
    const val PADDING_FILTER_TOP = 10   // Отступ сверху для фильтров (от заголовка)
    const val PADDING_FILTER_BOTTOM = 8 // Отступ снизу для фильтров (до сетки расписаний)
    
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
    
    // ID маршрутов (для type-safe использования)
    const val ROUTE_ID_102 = "102"                                 // Маршрут №102 (Славгород — Яровое)
    const val ROUTE_ID_102B = "102B"                               // Маршрут №102Б (Славгород — Яровое через Зори)
    const val ROUTE_ID_1 = "1"                                     // Маршрут №1 (Вокзал — Совхоз)
    
    // Удаленные маршруты (для очистки данных)
    const val REMOVED_ROUTE_ID_2 = "2"                             // Удаленный маршрут №2
    const val REMOVED_ROUTE_ID_3 = "3"                             // Удаленный маршрут №3
    const val REMOVED_ROUTE_ID_4 = "4"                             // Удаленный маршрут №4
    const val REMOVED_ROUTE_ID_5 = "5"                             // Удаленный маршрут №5
    
    // Названия остановок (используются в расписаниях)
    const val STOP_SLAVGOROD_RYNOK = "Рынок (Славгород)"           // Остановка "Рынок" в Славгороде
    const val STOP_YAROVOE_MCHS = "МСЧ-128 (Яровое)"               // Остановка "МСЧ-128" в Яровом
    const val STOP_YAROVOE_ZORI = "Ст. Зори (Яровое)"              // Остановка "Станция Зори" в Яровом
    const val STOP_ROUTE1_VOKZAL = "вокзал"                        // Вокзал для маршрута №1
    const val STOP_ROUTE1_SOVHOZ = "совхоз"                        // Совхоз для маршрута №1
    
    // Таймауты и задержки (в миллисекундах)
    const val UPDATE_CHECK_STARTUP_DELAY_MS = 5000L                // Задержка перед проверкой обновлений при запуске
    const val TIMER_UPDATE_INTERVAL_MS = 1000L                     // Интервал обновления таймера обратного отсчета
    const val MIN_LOADING_ANIMATION_MS = 1000L                     // Минимальная длительность анимации загрузки
}
