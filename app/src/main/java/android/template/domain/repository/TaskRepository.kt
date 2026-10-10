package android.template.domain.repository

import android.template.domain.models.TaskModel
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksStream(): Flow<List<TaskModel>>
    fun getTaskStream(taskId: String): Flow<TaskModel?>
    suspend fun getTasks(forceUpdate: Boolean = false): List<TaskModel>
    suspend fun getTask(taskId: String, forceUpdate: Boolean = false): TaskModel?
    suspend fun refresh()
    suspend fun refreshTask(taskId: String)
    suspend fun createTask(title: String, description: String): String
    suspend fun updateTask(taskId: String, title: String, description: String)
    suspend fun completeTask(taskId: String)
    suspend fun activateTask(taskId: String)
    suspend fun clearCompletedTasks()
    suspend fun deleteTask(taskId: String)
    suspend fun deleteAllTasks()
}