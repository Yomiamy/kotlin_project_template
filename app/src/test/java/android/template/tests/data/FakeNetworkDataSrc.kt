package android.template.tests.data

import android.template.infra.network.NetworkDataSource
import android.template.infra.network.NetworkTaskDto

class FakeNetworkDataSrc(
    var tasks: List<NetworkTaskDto> = emptyList(),
) : NetworkDataSource {

    var loadCount = 0
        private set

    override suspend fun loadTasks(): List<NetworkTaskDto> {
        loadCount++
        return tasks
    }

    override suspend fun saveTasks(newTasks: List<NetworkTaskDto>) {
        tasks = newTasks
    }
}