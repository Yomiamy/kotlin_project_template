package android.template.infra.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("select * from task")
    fun observeAll(): Flow<List<LocalTaskEntity>>

    @Query("select * from task where id = :taskId")
    fun observeById(taskId: String): Flow<LocalTaskEntity?>

    @Query("select * from task")
    suspend fun selectAll(): List<LocalTaskEntity>

    @Query("select * from task where id = :taskId")
    suspend fun selectById(taskId: String): LocalTaskEntity?

    @Upsert
    suspend fun upsert(taskEntity: LocalTaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<LocalTaskEntity>)

    @Query("update task set isCompleted = :completed where id = :taskId")
    suspend fun updateCompleted(taskId: String, completed: Boolean)

    @Query("delete from task where id = :taskId")
    suspend fun deleteById(taskId: String): Int

    @Query("delete from task where isCompleted = 1")
    suspend fun deleteCompleted(): Int

    @Query("delete FROM task")
    suspend fun deleteAll()
}