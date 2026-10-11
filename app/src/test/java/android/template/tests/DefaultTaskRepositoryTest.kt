package android.template.tests

import android.template.data.DefaultTaskRepository
import android.template.domain.models.TaskModel
import android.template.infra.local.LocalTaskEntity
import android.template.infra.network.NetworkTaskDto
import android.template.tests.data.FakeNetworkDataSrc
import android.template.tests.data.FakeTaskDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTaskRepositoryTest {

    private val localTask = LocalTaskEntity(
        id = "local-1",
        title = "Local",
        description = "Only in local",
        isCompleted = false,
    )
    private val remoteTask = NetworkTaskDto(
        id = "remote-1",
        title = "Remote",
        shortDescription = "Only in network",
    )

    private lateinit var taskDao: FakeTaskDao
    private lateinit var netDataSource: FakeNetworkDataSrc

    @Before
    fun setUp() {
        taskDao = FakeTaskDao(listOf(localTask))
        netDataSource = FakeNetworkDataSrc(listOf(remoteTask))
    }

    // backgroundScope 與 runTest 共用 scheduler， 測試結束自動取消；背景 sync 需用 runCurrent() 推進（advanceUntilIdle 不執行背景任務）
    private fun TestScope.createRepository() =
        DefaultTaskRepository(taskDao, netDataSource, backgroundScope)

    // region Stream：先訂閱，再寫入，驗證「已在觀察的人會收到新值」

    @Test
    fun getTasksStream_whenTaskCreated_emitsUpdatedList() = runTest {
        val repository = createRepository()
        val emissions = mutableListOf<List<TaskModel>>()
        // Unconfined：collector 立即執行，launch 返回時已收到初始值
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getTasksStream().toList(emissions)
        }
        assertEquals(listOf("local-1"), emissions.single().map { it.id })

        val id = repository.createTask(title = "New", description = "Desc")

        assertEquals(2, emissions.size)
        assertEquals(setOf("local-1", id), emissions.last().map { it.id }.toSet())
    }

    @Test
    fun getTaskStream_whenTaskDeleted_emitsNull() = runTest {
        val repository = createRepository()
        val emissions = mutableListOf<TaskModel?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getTaskStream("local-1").toList(emissions)
        }
        assertEquals("Local", emissions.single()?.title)

        repository.deleteTask("local-1")

        assertEquals(2, emissions.size)
        assertNull(emissions.last())
    }

    // endregion

    // region forceUpdate / refresh

    @Test
    fun getTasks_withoutForceUpdate_doesNotHitNetwork() = runTest {
        val repository = createRepository()

        val tasks = repository.getTasks(forceUpdate = false)

        assertEquals(0, netDataSource.loadCount)
        assertEquals(listOf("local-1"), tasks.map { it.id })
    }

    @Test
    fun getTasks_withForceUpdate_replacesLocalWithNetwork() = runTest {
        val repository = createRepository()

        val tasks = repository.getTasks(forceUpdate = true)

        assertEquals(1, netDataSource.loadCount)
        // local-only 的 task 被移除（replaceAll 而不是 upsertAll）
        assertEquals(listOf("remote-1"), tasks.map { it.id })
    }

    @Test
    fun refreshTask_missingOnServer_keepsLocal() = runTest {
        val repository = createRepository()

        repository.refreshTask("local-1")

        assertEquals("Local", repository.getTask("local-1", forceUpdate = false)?.title)
    }

    // endregion

    // region 寫入與背景同步

    @Test
    fun createTask_returnsBeforeSync_thenSyncsLatestSnapshot() = runTest {
        val repository = createRepository()

        val id = repository.createTask(title = "New", description = "Desc")
        // 寫入已返回，但背景 sync 還沒跑：網路仍是舊資料
        assertFalse(netDataSource.tasks.any { it.id == id })

        runCurrent()   // 執行背景 sync（advanceUntilIdle 不會跑 backgroundScope 的任務）

        assertEquals(setOf("local-1", id), netDataSource.tasks.map { it.id }.toSet())
    }

    @Test
    fun completeTask_updatesLocalAndNetwork() = runTest {
        val repository = createRepository()

        repository.completeTask("local-1")
        runCurrent()

        assertTrue(repository.getTask("local-1", forceUpdate = false)!!.isCompleted)
        assertEquals(
            NetworkTaskDto.TaskStatus.COMPLETE,
            netDataSource.tasks.single { it.id == "local-1" }.status,
        )
    }

    @Test(expected = NoSuchElementException::class)
    fun updateTask_unknownId_throws() = runTest {
        createRepository().updateTask(taskId = "missing", title = "x", description = "y")
    }

    // endregion
}