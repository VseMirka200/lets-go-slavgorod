package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.utils.Constants

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
 * @author VseMirka200
 * @version 2.0 (Database version 6)
 * @since 1.0
 */
@Database(
    entities = [FavoriteTimeEntity::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters()
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}