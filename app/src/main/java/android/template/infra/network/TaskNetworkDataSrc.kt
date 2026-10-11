package android.template.infra.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class TaskNetworkDataSrc @Inject constructor() : NetworkDataSource {
    companion object {
        private val SERVICE_LATENCY = 2.seconds
    }

    private val accessMutex = Mutex()

    private var tasks = listOf(
        NetworkTaskDto(
            id = "PISA",
            title = "Build tower in Pisa",
            shortDescription = "Ground looks good, no foundation work required.",
        ),
        NetworkTaskDto(
            id = "TACOMA",
            title = "Finish bridge in Tacoma",
            shortDescription = "Found awesome girders at half the cost!",
        ),
    )

    override suspend fun loadTasks(): List<NetworkTaskDto> = accessMutex.withLock {
        delay(SERVICE_LATENCY)
        tasks
    }

    override suspend fun saveTasks(newTasks: List<NetworkTaskDto>) = accessMutex.withLock {
        delay(SERVICE_LATENCY)
        tasks = newTasks
    }
}