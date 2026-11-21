package com.example.againdue.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Item::class, Completion::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    abstract fun completionDao(): CompletionDao

    companion object {
        private const val DATABASE_NAME = "againdue.db"

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN startDate INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN pattern TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN endDate INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN endAfterCount INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN recurrenceEnded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN completedCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE items SET startDate = lastDoneAt")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN reminderMinuteOfDay INTEGER")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
