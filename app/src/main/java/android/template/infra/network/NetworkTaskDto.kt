package android.template.infra.network

data class NetworkTaskDto(
    val id: String,
    val title: String,
    val shortDescription: String,
    val priority: Int? = null,
    val status: TaskStatus = TaskStatus.ACTIVE
) {
    enum class TaskStatus { ACTIVE, COMPLETE }
}