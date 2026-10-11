package android.template.tests.data

import android.template.infra.local.LocalTaskEntity
import android.template.infra.local.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeTaskDao(initial: List<LocalTaskEntity> = emptyList()) : TaskDao {

    private val data = MutableStateFlow(initial.associateBy { it.id })

    override fun observeAll(): Flow<List<LocalTaskEntity>> = data.map { it.values.toList() }

    override fun observeById(taskId: String): Flow<LocalTaskEntity?> = data.map { it[taskId] }

    override suspend fun selectAll(): List<LocalTaskEntity> = data.value.values.toList()

    override suspend fun selectById(taskId: String): LocalTaskEntity? = data.value[taskId]

    override suspend fun upsert(taskEntity: LocalTaskEntity) {
        data.update { it + (taskEntity.id to taskEntity) }
    }

    override suspend fun upsertAll(tasks: List<LocalTaskEntity>) {
        data.update { it + tasks.associateBy { task -> task.id } }
    }

    override suspend fun updateCompleted(taskId: String, completed: Boolean) {
        data.update { map ->
            val task = map[taskId] ?: return@update map
            map + (taskId to task.copy(isCompleted = completed))
        }
    }

    override suspend fun deleteById(taskId: String): Int {
        val existed = taskId in data.value
        data.update { it - taskId }
        return if (existed) 1 else 0
    }

    override suspend fun deleteCompleted(): Int {
        val completedIds = data.value.values.filter { it.isCompleted }.map { it.id }
        data.update { it - completedIds.toSet() }
        return completedIds.size
    }

    override suspend fun deleteAll() {
        data.value = emptyMap()
    }
    // replaceAll 沿用 interface 的 default 實作（deleteAll + upsertAll）
}