package android.template.infra.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalTaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ToDoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

}