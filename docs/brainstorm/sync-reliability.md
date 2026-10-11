# 背景同步可靠性（待修正）

狀態：已知缺陷，排定 Day 3 處理（選擇「先保留 `scope.launch`，Day 3 修正」）。
程式位置：`DefaultTaskRepository.syncToNetwork()` 的 `TODO`。

## 問題
1. 同步失敗只 log，沒有重試；server 停在舊資料直到下一次寫入。
2. 同步失敗後執行 `refresh()`，`replaceAll` 以 server 舊資料覆蓋尚未同步的 local 修改 → **資料遺失**。

## 預定解法
- [ ] `LocalTaskEntity` 新增 `isDirty`；Room migration v1 → v2，並 commit 新 schema。
- [ ] 寫入時標記 dirty，同步成功後清除。
- [ ] `replaceAll` 只刪除／覆寫 `isDirty = false` 的列。
- [ ] WorkManager unique work：網路 constraint + exponential backoff；Hilt WorkerFactory。
- [ ] `TaskDao.observeHasDirty(): Flow<Boolean>`，UI 顯示「尚未同步」。
- [ ] 測試：migration test、dirty 不被 refresh 覆蓋、worker 成功後清除 dirty。
