package android.template.data

import android.template.core.di.ApplicationScope
import android.template.data.extension.toDomain
import android.template.data.extension.toLocal
import android.template.data.extension.toNetwork
import android.template.domain.models.TaskModel
import android.template.domain.repository.TaskRepository
import android.template.infra.local.TaskDao
import android.template.infra.network.NetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

class DefaultTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val netDataSource: NetworkDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : TaskRepository {

    private val syncMutex = Mutex()

    override fun getTasksStream(): Flow<List<TaskModel>> =
        taskDao.observeAll().map { it.toDomain() }

    override fun getTaskStream(taskId: String): Flow<TaskModel?> =
        taskDao.observeById(taskId).map { it?.toDomain() }

    override suspend fun getTasks(forceUpdate: Boolean): List<TaskModel> {
        if (forceUpdate) refresh()
        return taskDao.selectAll().toDomain()
    }

    override suspend fun getTask(
        taskId: String,
        forceUpdate: Boolean
    ): TaskModel? {
        if (forceUpdate) refresh()
        return taskDao.selectById(taskId)?.toDomain()
    }

    override suspend fun refresh() {
        val networkTaskDtos = netDataSource.loadTasks()
        taskDao.replaceAll(networkTaskDtos.toLocal())
    }

    override suspend fun refreshTask(taskId: String) {
        val networkTaskDto = netDataSource.loadTasks().firstOrNull { it.id == taskId } ?: return
        taskDao.upsert(networkTaskDto.toLocal())
    }

    override suspend fun createTask(
        title: String,
        description: String,
    ): String {
        val task = TaskModel(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
        )
        taskDao.upsert(task.toLocal())
        syncToNetwork()
        return task.id
    }

    override suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
    ) {
        val localTaskEntity =
            taskDao.selectById(taskId) ?: throw NoSuchElementException("Task $taskId not found")
        taskDao.upsert(
            localTaskEntity.copy(
                title = title,
                description = description,
            )
        )
        syncToNetwork()
    }

    override suspend fun completeTask(taskId: String) {
        taskDao.updateCompleted(taskId = taskId, completed = true)
        syncToNetwork()
    }

    override suspend fun activateTask(taskId: String) {
        taskDao.updateCompleted(taskId = taskId, completed = false)
        syncToNetwork()
    }

    override suspend fun clearCompletedTasks() {
        taskDao.deleteCompleted()
        syncToNetwork()
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId = taskId)
        syncToNetwork()
    }

    override suspend fun deleteAllTasks() {
        taskDao.deleteAll()
        syncToNetwork()
    }

    /**
     * 將 local 的完整快照推送到網路。
     *
     * - 不 suspend：寫入方法呼叫後立即返回，同步在 [ApplicationScope] 背景執行，
     *   不受呼叫端（例如 viewModelScope）生命週期影響。
     * - [syncMutex] 包住「讀快照 + 寫網路」：最後一個執行的 sync 一定讀到最新的 local 狀態，
     *   不會有舊快照在新快照之後才寫入網路。
     *
     * TODO(randy): 已知缺陷，Day 3 修正，詳見 docs/brainstorm/sync-reliability.md
     *  1. 失敗只 log，沒有重試；server 停在舊資料直到下一次寫入。
     *  2. 失敗後若執行 refresh()，replaceAll 會以 server 舊資料覆蓋未同步的 local 修改（資料遺失）。
     *  解法方向：LocalTaskEntity.isDirty + Room migration、replaceAll 跳過 dirty 列、WorkManager 重試。
     */
    private fun syncToNetwork() {
        scope.launch {
            try {
                syncMutex.withLock {
                    val snapshot = taskDao.selectAll()
                    netDataSource.saveTasks(snapshot.toNetwork())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}