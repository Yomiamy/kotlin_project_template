# 分層架構與資料層決策

## 參考來源
- 工具鏈取自 [architecture-templates `base`](https://github.com/android/architecture-templates)：AGP 9、Kotlin 2.4、Room 2.8、Hilt、Navigation 3。
- 分層、資料流與測試策略參照 [architecture-samples](https://github.com/android/architecture-samples)。

## 分層與依賴方向

```
ui ──► domain ◄── data ──► infra
                            ├─ local   (Room：LocalTaskEntity / TaskDao / ToDoDatabase)
                            └─ network (NetworkTaskDto / NetworkDataSource / TaskNetworkDataSrc)
```

- `domain`：`TaskModel`、`TaskRepository` 介面。純 Kotlin，不 import `android.*` / `androidx.*` / 其他層。
- `data`：`DefaultTaskRepository` 與 mappers（`data/extension/TaskMappersExt.kt`，全部 `internal`）。唯一同時認得三種模型的層。
- `infra/local` 與 `infra/network` 互不依賴。
- 不做 data ↔ infra 的 port 反轉（data 直接依賴 infra 具體型別）；domain 不依賴任何層是唯一不可妥協的規則。

## 模型與轉換
- `TaskModel`（domain）↔ `LocalTaskEntity`（Room）↔ `NetworkTaskDto`（network）。
- 沒有 `TaskModel ↔ NetworkTaskDto` 的直接轉換：網路資料一律經過 local。
- `getTaskStream` / `observeById` 回傳 nullable：task 不存在是正常狀態。

## DI scope 原則
共用狀態，或「單一實例保證不在我們掌控的契約內」時，明確宣告 `@Singleton`：
- `ToDoDatabase`：連線池、invalidation tracker。
- `TaskDao`：不依賴 Room 內部對 DAO 的快取行為。
- `TaskNetworkDataSrc`：持有 fake server 資料與 Mutex。
- `DefaultTaskRepository`：持有 `syncMutex`。

Qualifier 使用 `AnnotationRetention.BINARY`；目前只有 `@ApplicationScope`，dispatcher qualifier 待有消費者時再加。

## 同步語意：offline-first
- 讀取一律從 local；只有 `forceUpdate` / `refresh()` 才拉網路，並以 `TaskDao.replaceAll`（`@Transaction`）覆蓋 local。
- 寫入：先 commit local（UI 透過 Flow 立即更新）＝成功；再於 `@ApplicationScope` 背景推送完整快照。
- `syncMutex` 包住「讀快照 + 寫網路」，確保最後執行的 sync 一定讀到最新 local 狀態。
- 已知缺陷見 `docs/brainstorm/sync-reliability.md`。

## 測試替身
- 有狀態依賴用 fake（`FakeTaskDao`、`FakeNetworkDataSrc`，Day 2 加 `FakeTaskRepository`）。
- 純互動驗證（analytics、logger）才考慮 MockK。
- fake 不具原子性；transaction 相關行為由 Room instrumented test 驗證。
