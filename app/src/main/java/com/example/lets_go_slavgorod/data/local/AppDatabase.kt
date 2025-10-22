package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.core.Constants

/**
 * Основная база данных приложения на базе Room
 * 
 * Управляет локальным хранилищем данных приложения с использованием
 * библиотеки Room Persistence Library. Обеспечивает типобезопасный
 * доступ к SQLite базе данных.
 * 
 * Содержит:
 * - Избранные времена отправления (FavoriteTimeEntity)
 * - DAO для работы с данными
 * - Миграции схемы базы данных
 * 
 * Особенности:
 * - Singleton паттерн для единственного экземпляра
 * - Автоматические миграции между версиями
 * - Thread-safe операции через @Volatile
 * - Поддержка резервного копирования
 * 
 * v2.0 Changes:
 * - Добавлена поддержка виджетов
 * - Улучшена производительность запросов
 * - Оптимизированы индексы для быстрого доступа
 * 
 * @author VseMirka200
 * @version 2.0 (Database version 6)
 * @since 1.0
 */
@Database(
    entities = [FavoriteTimeEntity::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Предоставляет доступ к DAO для работы с избранными временами
     * 
     * @return экземпляр FavoriteTimeDao для CRUD операций
     */
    abstract fun favoriteTimeDao(): FavoriteTimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Миграция базы данных с версии 4 на 5
         * 
         * Добавляет поля для хранения информации о маршруте:
         * - route_number: номер маршрута для отображения
         * - route_name: название маршрута для отображения
         * 
         * Это позволяет избежать дополнительных запросов к репозиторию
         * при отображении избранных времен.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE favorite_times ADD COLUMN route_number TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE favorite_times ADD COLUMN route_name TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Миграция базы данных с версии 5 на 6
         * 
         * Добавляет поле added_date для хранения даты добавления в избранное.
         * Используется для сортировки избранных времен по дате добавления.
         * 
         * Значение по умолчанию - текущий timestamp для существующих записей.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем поле added_date с текущим временем для существующих записей
                database.execSQL("ALTER TABLE favorite_times ADD COLUMN added_date INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        /**
         * Миграция базы данных с версии 6 на 7
         * 
         * Добавляет индексы для оптимизации производительности запросов:
         * - Индекс по route_id для быстрого поиска по маршруту
         * - Индекс по departure_time для сортировки по времени
         * - Индекс по day_of_week для фильтрации по дню недели
         * - Индекс по is_active для фильтрации активных записей
         * - Составные индексы для сложных запросов
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Создаем индексы для оптимизации производительности
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_route_id ON favorite_times (route_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_departure_time ON favorite_times (departure_time)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_day_of_week ON favorite_times (day_of_week)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_is_active ON favorite_times (is_active)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_route_id_is_active ON favorite_times (route_id, is_active)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_departure_time_day_of_week ON favorite_times (departure_time, day_of_week)")
            }
        }

        /**
         * Миграция базы данных с версии 7 на 8
         * 
         * Добавляет дополнительные индексы для оптимизации сложных запросов:
         * - Составной индекс по route_id и departure_time
         * - Составной индекс по is_active и departure_time
         * - Трехкомпонентный индекс для сложных запросов
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Создаем дополнительные индексы для оптимизации сложных запросов
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_route_id_departure_time ON favorite_times (route_id, departure_time)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_is_active_departure_time ON favorite_times (is_active, departure_time)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_times_route_id_day_of_week_is_active ON favorite_times (route_id, day_of_week, is_active)")
            }
        }

        /**
         * Получает единственный экземпляр базы данных (Singleton)
         * 
         * Использует double-checked locking для thread-safe инициализации.
         * Создает базу данных с поддержкой миграций и резервного копирования.
         * 
         * @param context контекст приложения для создания базы данных
         * @return экземпляр AppDatabase
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}