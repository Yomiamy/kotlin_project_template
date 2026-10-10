package android.template.data.extension

import android.template.domain.models.TaskModel
import android.template.infra.local.LocalTaskEntity
import android.template.infra.network.NetworkTaskDto

// TaskModel ↔ LocalTaskEntity ↔ NetworkTaskDto

// TaskModel ↔ LocalTaskEntity
internal fun TaskModel.toLocal() = LocalTaskEntity(id, title, description, isCompleted)
internal fun LocalTaskEntity.toDomain() = TaskModel(id, title, description, isCompleted)
internal fun List<LocalTaskEntity>.toDomain() = map(LocalTaskEntity::toDomain)

// LocalTaskEntity ↔ NetworkTaskDto（同步用）
internal fun LocalTaskEntity.toNetwork() = NetworkTaskDto(
    id = id, title = title, shortDescription = description,
    status = if (isCompleted) NetworkTaskDto.TaskStatus.COMPLETE else NetworkTaskDto.TaskStatus.ACTIVE,
)

internal fun List<LocalTaskEntity>.toNetwork() = map(LocalTaskEntity::toNetwork)

internal fun NetworkTaskDto.toLocal() = LocalTaskEntity(
    id = id, title = title, description = shortDescription,
    isCompleted = status == NetworkTaskDto.TaskStatus.COMPLETE,
)

internal fun List<NetworkTaskDto>.toLocal() = map(NetworkTaskDto::toLocal)