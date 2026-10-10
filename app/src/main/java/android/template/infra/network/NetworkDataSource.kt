package android.template.infra.network

interface NetworkDataSource {
    suspend fun loadTasks(): List<NetworkTaskDto>
    suspend fun saveTasks(newTasks: List<NetworkTaskDto>)
}