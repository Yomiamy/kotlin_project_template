package android.template.core.di

import android.content.Context
import android.template.infra.local.TaskDao
import android.template.infra.local.ToDoDatabase
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "Tasks.db"


    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): ToDoDatabase =
        Room.databaseBuilder(
            context = context,
            klass = ToDoDatabase::class.java,
            name = DATABASE_NAME,
        ).build()

    @Singleton
    @Provides
    fun provideTaskDao(database: ToDoDatabase): TaskDao = database.taskDao()
}