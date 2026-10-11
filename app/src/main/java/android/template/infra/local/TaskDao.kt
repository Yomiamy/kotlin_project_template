package android.template.infra.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    /**
     * 以 [tasks] 完整取代本地資料。包在 transaction 裡：
     * observer 不會看到中間的空 list，中途失敗也不會留下被清空的表。
     */
    @Transaction
    suspend fun replaceAll(tasks: List<LocalTaskEntity>) {
        deleteAll()
        upsertAll(tasks)
    }
}