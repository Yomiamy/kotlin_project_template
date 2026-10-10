---
trigger: always_on
---

# Android / Kotlin 專案 Style Guide

本 Style Guide 旨在為 Android / Kotlin 專案提供一致的程式碼風格、架構設計準則與最佳實踐。本規範深度整合 Android 官方架構指南 (Guide to App Architecture)、Jetpack Compose 官方指南、Effective Kotlin (Marcin Moskala)、Effective Android 以及 Linus Torvalds 的「好品味」與「實用主義」哲學，以確保專案具備極高可讀性、型別安全、優異效能與向後相容性。

Kotlin 版本 2.0+ ｜ Android compileSdk / targetSdk 35+ ｜ Jetpack Compose (Material 3)

---

## 🎯 Android & Kotlin 專家規範 (Execution)

在 Linus 哲學的指導下，執行具體的 Android / Kotlin 開發任務。

### 🧠 技術一般原則
- **先思考**：編寫程式碼前，先分析 `build.gradle.kts` 與 `libs.versions.toml`，掌握既有依賴庫與版本，嚴禁無謂引進重複或衝突的第三方套件。
- **整潔架構 (Clean Architecture)**：嚴格分離 UI 表現層 (UI / Presentation Layer)、領域層 (Domain Layer) 與數據層 (Data Layer)。依賴單向向內，UI 層絕不直接存取 Data Source。
- **單向資料流 (UDF)**：狀態向下傳遞 (State Down)，事件向上回發 (Events Up)。UI 狀態由單一真實來源 (Single Source of Truth, SSOT) 控制。
- **不可變性優先 (Immutability by Default)**：遵循 Effective Kotlin Item 1，變數一律優先 `val`，集合優先不可變 (`List`, `Set`, `Map`)，模型優先 `data class`。
- **Null 安全**：充分運用 Kotlin 編譯期 Null Safety。除非在單元測試斷言或能 100% 證明不為 null，否則**嚴禁使用 `!!` (雙驚嘆號)**；使用安全呼叫 `?.`、Elvis 運算子 `?:` 或衛語句提早返回。

### 💻 編碼標準
- **Compose 穩定性 (Stability)**：傳入 Composable 的參數必須為穩定型別 (`@Stable` / `@Immutable`)。避免在 Composable 重組範圍內傳入不穩定集合或重複分配 lambda。
- **元件拆分與狀態提升 (State Hoisting)**：將複雜 UI 拆解為小而單一職責的 Composable。區分無狀態 (Stateless) 與有狀態 (Stateful) 元件，優先暴露無狀態元件以利預覽與測試。
- **Edge-to-Edge 與 Insets (Android 15+)**：Android 15 (API 35) 強制開啟邊到邊版面。Activity 必須呼叫 `enableEdgeToEdge()`，UI 根部必須妥善處理 WindowInsets（如 `statusBarsPadding()`, `navigationBarsPadding()`, `imePadding()`）。
- **格式化與習慣法**：遵守 Kotlin 官方 Coding Conventions 與 ktlint 規範。保持尾隨逗號 `,` 以確保 Git diff 整潔。
- **命名規範**：類別/介面使用 `PascalCase`，一般函式/變數使用 `camelCase`，Composable 函式（無回傳值 UI 節點）使用 `PascalCase`，編譯期常數使用 `SCREAMING_SNAKE_CASE`。
- **非同步與協程**：全面採用 Kotlin Coroutines 與 Flow。**嚴禁使用 `GlobalScope`**。UI 層收集 Flow 必須透過 `collectAsStateWithLifecycle` 保證生命週期安全。

### 🛠 架構與狀態管理
- **架構選型**：現代 Android MVVM / MVI 模式。
  - **ViewModel**：負責管理 UI 狀態與調配業務邏輯。冷串流透過 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InitialState)` 轉換為熱狀態，有效節約背景資源並抵抗螢幕旋轉。
  - **UI 狀態建模**：使用 `sealed interface` 窮盡建模 UI 狀態（例如 `Loading`, `Success`, `Error`），消滅中間非法狀態。
  - **單次事件處理 (One-off Events)**：導航、Toast、SnackBar 等事件，透過帶有消耗狀態的 UI State 或 `Channel` 處理，避免螢幕旋轉或重組時重複觸發。
  - **狀態恢復 (Process Death)**：關鍵 UI 狀態在 ViewModel 中透過 `SavedStateHandle` 保留；在 Composable 中透過 `rememberSaveable` 搭配自訂 `Saver` 或 `@Parcelize` 恢復。
- **UI 純淨原則**：Composable 函式必須是「狀態到 UI 的純粹映射」，嚴禁在 Composable 函式主體中直接觸發非同步網路請求或資料庫修改，所有副作用必須進入 `LaunchedEffect`、`DisposableEffect` 或委派給 ViewModel。

### 📦 關鍵庫與技術棧偏好
- **UI**：Jetpack Compose + Material 3
- **導航**：Navigation Compose (基於 `@Serializable` 之 Type-Safe 路由)
- **依賴注入**：Hilt (Dagger Hilt)
- **非同步與串流**：Kotlin Coroutines + Flow
- **序列化**：`kotlinx.serialization` (優先於 Gson/Moshi)
- **本地持久化**：Room (關聯式資料) / Jetpack DataStore Preferences (鍵值配置)
- **圖片載入**：Glide Compose 或 Coil

### 🚀 優化與測試
- **優化**：善用 `derivedStateOf` 抑制高頻重組；在 `LazyColumn` / `LazyRow` 強制設定 `key` 與 `contentType`；延遲狀態讀取（Lambda 修飾符）以跳過重組階段進入繪製階段。
- **測試**：編寫 Unit Tests (ViewModel, Repository, UseCase) 與 Compose UI Tests。非同步與 Flow 測試一律搭配 `runTest` 與 Turbine。

---

### 🔧 工具使用指南 (Agent Tools)

*   **語義化程式碼 (Semantic Code)**：像使用 IDE 一樣思考。利用符號搜尋與引用追蹤工具理解上下文。
*   **依賴優先**：先查閱 `gradle/libs.versions.toml` 確認現有庫版本，避免引入不相容依賴。
*   **Linus 簡潔準則**：巢狀縮排不超過 3 層，單一函式超過 50 行即檢視是否拆分，消除多餘邊界分支。

---

## 1. 格式化與代碼風格 (Formatting & Code Style)

### 1.1. 使用官方代碼風格與 ktlint

始終遵守 Kotlin 官方編碼慣例。專案透過 ktlint 或 Android Studio 內建格式化工具維持一致風格。

### 1.2. 行長度與縮排

- 建議單行長度不超過 100~120 個字元。
- 縮排一律使用 4 個空格，禁止使用 Tab。
- 遵循衛語句 (Guard Clauses) 與提早返回 (Early Return)，避免深層縮排（最深不超過 3 層）。

### 1.3. 尾隨逗號 (Trailing Commas)

在多行參數宣告、引數呼叫、列舉常數與集合宣告時，始終在最後一項添加尾隨逗號 `,`。這能大幅降低版本控制中程式碼合併衝突 (Merge Conflict) 的機率。

```kotlin
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
)
```

---

## 2. 命名規範 (Naming Conventions)

### 2.1. 類別、介面、列舉與 Sealed 階層

使用 `PascalCase` (大駝峰命名法)。

```kotlin
class UserRepositoryImpl : UserRepository
interface PaymentProcessor
enum class NetworkState
sealed interface ScreenState
```

### 2.2. 函式與方法

- **一般函式與方法**：使用 `camelCase` (小駝峰命名法)，動詞或動賓片語優先。
- **Composable 函式 (回傳 Unit)**：使用 `PascalCase`，名詞或名詞修飾語優先。
- **Composable 函式 (具有回傳值)**：使用 `camelCase`（如 `rememberScrollState()`）。

```kotlin
fun calculateTotalPrice(): Double { /*...*/ }

@Composable
fun UserAvatar(imageUrl: String, modifier: Modifier = Modifier) { /*...*/ }

@Composable
fun rememberUserState(): UserState { /*...*/ }
```

### 2.3. 變數、屬性與 Backing Properties

- 一般變數與屬性使用 `camelCase`。
- **Backing Properties (內部可變、外部唯讀)**：私有可變屬性前綴底線 `_`，公開唯讀屬性無底線。

```kotlin
private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
```

### 2.4. 常數

- `const val` 編譯期常數或物件內的靜態常數使用 `SCREAMING_SNAKE_CASE`。
- 不可變物件實例（如 logger、非 primitive 常數）可用 `camelCase`。

```kotlin
const val MAX_RETRY_COUNT = 3
const val DEFAULT_TIMEOUT_MILLIS = 5000L
```

### 2.5. 檔案與套件命名

- **Kotlin 檔案**：與內部主要類別同名（`PascalCase.kt`）。包含多個頂層函式或擴充函式時，使用話題名加後綴（如 `StringExtensions.kt`, `DateUtils.kt`）。
- **套件名稱 (Package)**：一律全小寫單詞，不使用底線（如 `com.example.myapp.feature.auth`）。

### 2.6. 建構式與工廠函式

- 優先使用 Primary Constructor（主建構函式）。
- 複雜物件創建優先使用 Companion Object 工廠函式或 `operator fun invoke`，避免繁複的 secondary constructors。

```kotlin
class Password private constructor(val value: String) {
    companion object {
        fun of(raw: String): Result<Password> {
            return if (raw.length >= 8) {
                Result.success(Password(raw))
            } else {
                Result.failure(IllegalArgumentException("Password too short"))
            }
        }
    }
}
```

### 2.7. 參數設計 (Effective Kotlin)

1. **預設參數優於重載**：優先使用預設引數 (Default Arguments) 取代 Java 風格的多載函式。
2. **具名參數 (Named Arguments)**：
   - 參數數量 **≥ 3 個** 時，呼叫端應使用具名引數。
   - 包含多個相同型別（特別是連續的 `Boolean` 或 `String`）時，**強制使用具名引數**，杜絕語意混淆。
   - 當參數數量超過 4~5 個且常共同出現時，重構為獨立的參數模型 (Data Class)。
3. **Trailing Lambda**：當函式最後一個參數為 Lambda 時，將其移出括號外；若只有一個 Lambda 參數，則省略圓括號。

```kotlin
fun updateProfile(
    userId: String,
    displayName: String,
    isVerified: Boolean = false,
    isActive: Boolean = true,
)

updateProfile(
    userId = "U123",
    displayName = "Linus",
    isVerified = true,
    isActive = true,
)
```

---

## 3. 導入 (Imports)

### 3.1. 組織導入順序

1. `android.*` / `androidx.*` 核心與官方庫
2. 第三方庫 (如 `kotlinx.*`, `dagger.hilt.*`, `coil.*`, `retrofit2.*`)
3. 專案內部套件導入

區塊間以空行分隔。

### 3.2. 嚴禁萬用字元導入 (No Wildcard Imports)

**嚴格禁止** 使用 `import foo.bar.*`。萬用字元導入會污染命名空間並導致命名衝突。

---

## 4. 註解與 KDoc (Comments & KDoc)

### 4.1. KDoc 文件註解 (`/** ... */`)

針對以下情境必須撰寫 KDoc：
- **公共 API、架構基類與介面契約**：解釋其設計目的與實作預期。
- **複雜演算法或商業邏輯**：解釋「為什麼這樣做」，而非單純翻譯程式碼在做什麼。
- **公開擴充函式**：註明參數限制、異常行為與傳回值意義。

無須撰寫 KDoc 的情境：
- 命名極其自明的私有方法或 getter/setter。
- 單純覆寫介面的方法（除非具備特殊行為）。
- 簡單的 DTO、Entity 與 sealed 狀態子類別。

```kotlin
/**
 * 處理使用者認證與 Session 更新的領域層 UseCase。
 *
 * @property authRepository 驗證資料來源契約
 * @property tokenStorage 安全憑證存儲介面
 */
class AuthenticateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
)
```

### 4.2. 行內註解與 TODO

- 使用 `//` 標註非顯而易見的業務背景或特定 Bug Workaround。
- 使用 `// TODO(username): description` 標註待辦，必須附帶原因或 Ticket ID。

---

## 5. 程式碼結構與現代 Kotlin 特性 (Modern Kotlin Idioms)

### 5.1. 類別成員組織順序

1. 伴隨物件常數與工廠 (`companion object`)
2. 屬性 (常數 -> 公開屬性 -> 私有屬性 -> Backing Properties)
3. 建構函式 (`init` 區塊)
4. 公開方法 (`public methods`)
5. 私有輔助方法 (`private methods`)

### 5.2. `val` vs `var` 與限制可變性 (Effective Kotlin Item 1)

> 「爛程式員擔心程式碼。好程式員擔心資料結構。」—— Linus Torvalds

- **不可變性優先**：預設宣告 `val`。只有在確切需要重新指派時才使用 `var`。
- **不可變集合**：暴露給外部的集合一律為唯讀 `List`、`Set`、`Map`。若內部使用 `toMutableList()`，必須限制在其區域變數範圍內。
- **使用 `copy()` 變更狀態**：透過資料類別的 `copy()` 產生新實例，而非就地變異 (Mutating in-place)。

```kotlin
// Bad: 暴露可變集合讓外部破壞資料一致性
class CartStore {
    val items = mutableListOf<CartItem>() // ❌ 外部可隨意操作
}

// Good: 內部封裝，外部唯讀
class CartStore {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()
}
```

### 5.3. 複雜度限制、消除巢狀與衛語句 (Linus 哲學)

> 「如果你需要超過 3 層的縮排，那你已經搞砸了，應該去修好你的程式。」—— Linus Torvalds

- **消滅 Else**：優先採用衛語句 (Guard Clauses / Early Return) 提早中斷處理。
- **使用 `require` / `check` 驗證契約** (Effective Kotlin Item 5)：
  - `require(condition)`：檢驗引數合法性（不符時拋出 `IllegalArgumentException`）。
  - `check(condition)`：檢驗內部狀態合法性（不符時拋出 `IllegalStateException`）。

```kotlin
// Bad: 深層巢狀
fun processOrder(order: Order?, user: User?) {
    if (order != null) {
        if (user != null) {
            if (user.hasCredit()) {
                execute(order)
            }
        }
    }
}

// Good: 扁平衛語句，縮排維持 1 層
fun processOrder(order: Order?, user: User?) {
    if (order == null || user == null) return
    require(user.hasCredit()) { "User does not have sufficient credit" }
    execute(order)
}
```

### 5.4. 現代 Kotlin 特性實踐 (Kotlin 2.0+)

#### 5.4.1. `sealed interface` / `sealed class` 與窮盡式 `when`

- 定義狀態階層時，優先使用 `sealed interface`（節省類別繼承開銷並允許實現多個介面）。
- 搭配無 `else` 分支的窮盡式 `when` 表達式。當未來新增子型別時，編譯器將強制要求處理，消滅漏判風險。
- 無狀態的子分支一律使用 `data object` (Kotlin 1.9+)，具備乾淨的 `toString()` 與單例特性。

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val throwable: Throwable) : UiState<Nothing>
}

fun render(state: UiState<String>) = when (state) {
    UiState.Loading -> showSpinner()
    is UiState.Success -> showContent(state.data)
    is UiState.Error -> showError(state.throwable.message.orEmpty())
}
```

#### 5.4.2. `value class` (內聯類別)

針對業務領域中的強型別包裝（如 `UserId`, `OrderId`, `Money`），使用 `@JvmInline value class`。在編譯期具備型別檢查安全，運行期則被展開為原始型別，**零物件配置成本**。注意：當 value class 被當作泛型或介面型別傳遞時仍會發生裝箱 (boxing)，應避免無謂轉型。

```kotlin
@JvmInline
value class UserId(val raw: String)

@JvmInline
value class OrderId(val raw: String)

fun fetchUserOrders(userId: UserId) { /*...*/ }
```

#### 5.4.3. 作用域函式 (Scope Functions) 的精確選型

| 函式 | Context 物件 | 傳回值 | 主要使用情境 |
|---|---|---|---|
| `apply` | `this` | Context 物件 | 物件初始化與配置（如建立 View / Intent / 物件設定） |
| `also` | `it` | Context 物件 | 附加副作用（如除錯日誌、資料驗證） |
| `let` | `it` | Lambda 結果 | 唯二合法用途：(1) Null 安全變換 `obj?.let { }` (2) 區域變數作用域限制 |
| `run` | `this` | Lambda 結果 | 物件配置並立即計算傳回值 |
| `with` | `this` | Lambda 結果 | 對同一物件連續呼叫多個方法（非 extension） |

**禁止反模式**：嚴禁將多個 `let`、`run`、`apply` 巢狀串接超過 2 層。

#### 5.4.4. 擴充函式 (Extension Functions) 的節制

- 僅在擴充函式概念上「屬於接收者型別通用工具」時才定義為公開擴充。
- 專屬於特定領域或畫面的轉換邏輯，應作為私有方法或放在獨立的 Mapper 類別中，嚴禁污染全域標準函式庫型別（如任意擴充 `String`, `Int`, `Context`）。

---

## 6. 錯誤處理 (Error Handling & Exceptions)

### 6.1. 錯誤處理哲學 (Effective Kotlin Item 7)

- **預期內的業務失敗**：優先回傳 `Result<T>` 或封裝自訂的 `sealed interface Outcome<out T>`，而非拋出例外 (Exceptions)。
- **不可恢復的程式錯誤 / 系統錯誤**：使用 `error()`、`throw IllegalStateException()` 明確中斷。

```kotlin
suspend fun fetchUserProfile(userId: String): Result<UserProfile> {
    return runCatching {
        api.getUser(userId).toDomain()
    }
}
```

### 6.2. 捕捉具體例外與協程取消安全

- **具體優先**：捕捉例外時，明確指明例外型別（如 `on IOException`, `catch (e: HttpException)`）。
- **嚴禁生吞 `CancellationException`**：在協程環境中，`CancellationException` 是協程結構化取消的信號機制。若捕捉了泛用 `Throwable` 或 `Exception`，**必須重新拋出 `CancellationException`**，否則會破壞協程生命週期管理並導致記憶體洩漏！

```kotlin
try {
    doAsyncWork()
} catch (e: CancellationException) {
    throw e // ✅ 保持協程取消鏈暢通
} catch (e: IOException) {
    Timber.e(e, "Network I/O failed")
}
```

### 6.3. 協程錯誤邊界 (`supervisorScope` vs `coroutineScope`)

- 子任務相互獨立（單一失敗不應連鎖拖垮其他任務）時，使用 `supervisorScope`。
- 子任務為一體（任一失敗整個操作應全部取消）時，使用 `coroutineScope`。

### 6.4. 日誌規則 (Logging)

- 使用 `Timber` 或統一的 Logger 封裝；**嚴禁直接使用 `println()`**。
- 僅在 Debug 模式下輸出偵錯日誌，Release 構建必須透過 ProGuard / R8 規則排除。

---

## 7. Android & Jetpack Compose 核心準則 (Jetpack Compose Specifics)

### 7.1. 元件拆分與狀態提升 (State Hoisting)

- **有狀態與無狀態分離**：
  - **Stateful Composable**：負責管理/注入 ViewModel、生命週期狀態、事件回調路由。
  - **Stateless Composable**：純粹接收資料模型與 lambda 回呼，不依賴任何特定 ViewModel 或架構元件。具備極高的可複用性與 Preview 能力。
- **Slot API 模式**：利用 `@Composable () -> Unit` 作為子元件插槽，實現靈活佈局組合。

```kotlin
@Composable
fun UserProfileRoute(
    viewModel: UserProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UserProfileScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onBackClick = onNavigateBack,
    )
}

@Composable
fun UserProfileScreen(
    uiState: UserProfileUiState,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { ProfileTopBar(onBackClick = onBackClick) },
    ) { innerPadding ->
        // ...
    }
}
```

### 7.2. Modifier 設計規範

1. **首個選填參數**：每個對外公開的 Composable 函式都**必須**包含 `modifier: Modifier = Modifier` 參數，並且放在所有必填參數之後的**第一個可選參數**。
2. **傳遞至最外層佈局**：傳入的 `modifier` 必須指派給該 Composable 的根 Layout 節點。
3. **修飾鏈順序至關重要**：Modifier 的鏈接順序決定了測量與繪製順序。例如 `padding` 在 `clickable` 之前與之後效果截然不同。

```kotlin
@Composable
fun OrderCard(
    order: Order,
    onOrderClick: (OrderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOrderClick(order.id) }
            .padding(16.dp),
    ) {
        // ...
    }
}
```

### 7.3. 重組 (Recomposition) 與效能優化 (Effective Compose)

#### 7.3.1. 穩定性契約 (Stability & Immutable)

Compose 編譯器依據參數是否「穩定 (Stable)」決定是否跳過 (Smart Recomposition Skip) 該元件：
- 基本型別 (Primitive types)、`String`、函式型別預設為穩定。
- **Kotlin 標準集合 (`List<T>`, `Set<T>`, `Map<T>`) 在 Compose 中被視為不穩定 (Unstable)**。
- **解決方案**：
  1. 使用 kotlinx-collections-immutable 的 `ImmutableList<T>`。
  2. 或在包含集合的 UI 狀態 Model 上標註 `@Immutable` / `@Stable`。

```kotlin
@Immutable
data class UserListUiState(
    val users: List<User> = emptyList(),
)
```

#### 7.3.2. `derivedStateOf` 的正確使用時機

- **唯一正確時機**：當某個狀態更新的頻率遠高於實際需要重組 UI 的頻率時。
- **典型情境**：滾動位置、清單第 N 項是否可見。

```kotlin
val listState = rememberLazyListState()

val showScrollToTopButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

#### 7.3.3. 副作用中 Lambda 穩定性 (`rememberUpdatedState`)

當在長時間運行的 `LaunchedEffect` 或回呼中引用可能變更的參數時，使用 `rememberUpdatedState`，避免因參數更新而頻繁重啟 Effect。

```kotlin
@Composable
fun LandingScreen(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) {
        delay(SplashWaitMillis)
        currentOnTimeout()
    }
}
```

#### 7.3.4. 列表效能：強制 `key` 與 `contentType`

在 `LazyColumn` / `LazyRow` 中：
- 必須提供穩定且唯一的 `key`。
- 當列表包含多種不同佈局項目時，必須指定 `contentType`。

```kotlin
LazyColumn {
    items(
        items = messages,
        key = { message -> message.id },
        contentType = { message -> message.type },
    ) { message ->
        MessageRow(message = message)
    }
}
```

### 7.4. 狀態恢復與行程終止 (Process Death Restoration)

- **UI 元素層級**：使用 `rememberSaveable`。非基本型別需實作自訂 `Saver` 或 `@Parcelize`。
- **ViewModel 層級**：透過注入 `SavedStateHandle`，確保 Process Death 重建後狀態能自 Bundle 恢復。

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    fun onQueryChange(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
    }
}
```

### 7.5. Type-Safe Navigation Compose (Navigation 2.8+ / 2.9+)

專案應全面捨棄脆弱的字串路徑拼接（如 `"user/{id}"`），改用 Kotlinx Serialization 定義強型別路由：

```kotlin
@Serializable
data object HomeRoute

@Serializable
data class ProfileRoute(val userId: String)

NavHost(navController = navController, startDestination = HomeRoute) {
    composable<HomeRoute> {
        HomeScreen(onNavigateToProfile = { id -> navController.navigate(ProfileRoute(id)) })
    }
    composable<ProfileRoute> { backStackEntry ->
        val route: ProfileRoute = backStackEntry.toRoute()
        ProfileScreen(userId = route.userId)
    }
}
```

### 7.6. Context 與生命週期安全 (Lifecycle & Async Gaps)

- **嚴禁持有 Activity Context**：ViewModel、Repository、單例或長時間存活的 Coroutine 嚴禁持有 Activity Context 或 View 實例，避免造成記憶體洩漏。
- **生命週期感知收集**：在 Composable 中收集 Flow，**一律使用 `collectAsStateWithLifecycle()`**，嚴禁在 UI 層裸用 `collectAsState()`。

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

### 7.7. ViewModel 最佳實踐與 `WhileSubscribed(5_000)`

在 ViewModel 中將 Repository 的冷串流轉為熱 UI 狀態時，必須使用 `SharingStarted.WhileSubscribed(5_000)`。它能在 Configuration Change（如螢幕旋轉）發生時維持訂閱不停，並在 App 真正切至背景超過 5 秒後自動取消上游數據收集，兼顧效能與省電。

```kotlin
@HiltViewModel
class CartViewModel @Inject constructor(
    getCartUseCase: GetCartUseCase,
) : ViewModel() {
    val uiState: StateFlow<CartUiState> = getCartUseCase()
        .map { CartUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CartUiState.Loading,
        )
}
```

---

## 8. 協程與非同步串流 (Coroutines & Kotlin Flow)

### 8.1. 結構化併發 (Structured Concurrency)

- **嚴禁 `GlobalScope`**。
- 使用對應生命週期的 Scope（如 `viewModelScope`, `lifecycleScope`），或使用 `coroutineScope { }` / `supervisorScope { }`。

### 8.2. Dispatcher 分工與依賴注入

- `Dispatchers.Main.immediate`：UI 操作與快速狀態更新。
- `Dispatchers.IO`：磁碟讀寫、檔案操作、網路請求。
- `Dispatchers.Default`：CPU 密集計算、大型集合過濾排序、JSON 序列化解析。
- **Dispatcher 注入原則**：類別中**嚴禁硬編碼 `Dispatchers.IO`**，必須透過 Hilt 或建構子注入 Dispatcher，以利單元測試替換為 `TestDispatcher`。

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

### 8.3. StateFlow vs SharedFlow vs Channel

- **`StateFlow`**：代表「狀態」。有初始值，自動 Conflate，保留最新狀態（Replay = 1）。適用於 UI State。
- **`SharedFlow`**：代表「事件廣播」。無初始值，多個訂閱者均可收到廣播。
- **`Channel`**：代表「單次任務/隊列」。單一觀察者消費模型，適合用於導航命令、顯示單次 SnackBar 等 UI Event。

### 8.4. 協同式取消 (Cooperative Cancellation)

長時間執行的循環計算必須在迴圈中定期呼叫 `ensureActive()` 或 `yield()`。

---

## 9. 依賴注入 (Hilt / Dependency Injection)

### 9.1. 建構子注入優先 (Constructor Injection)

絕大多數類別使用 `@Inject constructor(...)` 進行建構子注入。

### 9.2. Scope 最小化原則

- 預設保持 **無 Scope (Unscoped)**。
- 僅在真正需要時使用 `@Singleton`、`@ActivityRetainedScoped` 或 `@ViewModelScoped`。

### 9.3. 介面綁定：`@Binds` 優於 `@Provides`

當需要將介面綁定至其具體實作時，始終在 `abstract class` module 中使用 `@Binds`，避免生成無謂的 Provider 工廠方法代碼。

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl,
    ): UserRepository
}
```

---

## 10. 測試架構與最佳實踐 (Testing)

### 10.1. 測試金字塔

- **單元測試 (Unit Tests)**：覆蓋 70%+。測試 ViewModel、UseCase、Repository。
- **元件測試 (Compose UI Tests)**：測試 Composable 渲染與互動。
- **整合測試 (Instrumentation Tests)**：驗證跨模組流程。

### 10.2. 單元測試與 Flow 測試

- 使用 `kotlinx-coroutines-test` 的 `runTest` 執行協程測試。
- 測試 `Flow` 時，**使用 Turbine 套件** 進行順序斷言。

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `incrementCounter emits incremented value`() = runTest(testDispatcher) {
        val viewModel = CounterViewModel()

        viewModel.uiState.test {
            assertEquals(0, awaitItem().count)
            viewModel.increment()
            assertEquals(1, awaitItem().count)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 10.3. 測試資料管理

將測試用模擬資料集中管理於 `TestDataFactory` 或 Object Mother 物件中。

### 10.4. Compose UI 測試

使用 `createComposeRule()`，透過 Semantics 節點比對而非脆弱的座標比對。

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginButton_whenClicked_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            LoginButton(onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("登入").performClick()
        assertTrue(clicked)
    }
}
```

---

## X. 架構決策與資料層 (Architecture Decisions & Data Layer)

### X.1. DTO / Entity / UI Model 三層嚴格分離

- **Data Transfer Object (DTO)**：映射自網路 API 或外部 JSON，使用 `@Serializable`。只存在於 Data Layer。
- **Domain Entity**：業務實體，代表純粹的商業規則。
- **UI State Model**：專為畫面呈現在 UI Layer 設計的模型。
- **隔離原則**：Data Layer 必須提供 Mapper 將 DTO 轉換為 Domain Entity，**嚴禁將 API DTO 直接穿透傳入 Compose UI 層**。

```kotlin
@Serializable
data class UserDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
) {
    fun toDomain(): User = User(id = userId, name = fullName)
}

data class User(val id: String, val name: String)
```

### X.2. 本地持久化：Room 與 DataStore

- **Room**：處理關聯型、結構化快取資料。DAO 查詢回傳 `Flow<List<T>>`。
- **DataStore Preferences**：處理輕量設定（Token、偏好），完全取代過時的 `SharedPreferences`。

---

## Y. 其他與反模式檢核 (Miscellaneous & Anti-Patterns)

### Y.1. 避免魔術數字與字串

- UI 尺寸與顏色定義在專案 Theme 或常數中，邊距使用 `dp` 常數。

### Y.2. 文字常數與多語系 (Strings & Localization)

- **UI 呈現文字一律使用字串資源 `stringResource(R.string.xxx)`**，嚴格禁止在 Composable 中寫死文字。
- 異常類別與底層 Log 的技術除錯字串使用英文。

### Y.3. Android / Kotlin 常見反模式檢核表 (Code Smells Checklist)

| 反模式 (Code Smell) | 為什麼是垃圾 (Why it's bad) | 正確做法 (Good Taste Solution) |
|---|---|---|
| **!! (雙驚嘆號強轉)** | 破壞 Kotlin 的 Null 安全承諾，生產環境 NPE 最大凶手。 | 使用 `?.`、`?:` 或提前以衛語句 `requireNotNull()` 驗證。 |
| **在 ViewModel 保存 Context / View** | 造成 Activity 無法回收，引發嚴重記憶體洩漏。 | ViewModel 只依賴 UseCase / Repository；需要 Context 的操作移回 UI 層。 |
| **Composable 內裸用 `collectAsState()`** | 背景運行時依然持續收集 Flow，空耗 CPU 與耗電。 | 使用 `collectAsStateWithLifecycle()`。 |
| **直接暴露 `MutableStateFlow`** | 外部任何呼叫者皆可任意發射新值，破壞單向資料流與封裝。 | 私有 `_state`，對外公開 `state.asStateFlow()` 唯讀介面。 |
| **硬編碼 `Dispatchers.IO`** | 無法在單元測試中替換為 `TestDispatcher`，導致測試脆弱易超時。 | 透過 Hilt 注入具名 Qualifier 的 `CoroutineDispatcher`。 |
| **未指定 `WhileSubscribed(5000)`** | 螢幕旋轉導致上游重複觸發，或進入背景持續拉取資料。 | 使用 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ...)`。 |
| **嵌套 `let` / `run` 超過 2 層** | 語義不明，作用域混亂。 | 展開為傳統衛語句或提取為命名良好的輔助函式。 |
| **忽略 WindowInsets 造成邊界穿透** | 在 Android 15+ 導致狀態列或導航列與內容發生重疊遮擋。 | 在 Scaffold 或根節點配置 `Modifier.statusBarsPadding()` 或 `safeDrawingPadding()`。 |

---

## 資料來源 (References)

- [Kotlin Official — Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Effective Kotlin: Best Practices](https://kt.academy/book/effectivekotlin) (Marcin Moskala)
- [Android Developers — Guide to app architecture](https://developer.android.com/topic/architecture)
- [Android Developers — Jetpack Compose best practices](https://developer.android.com/develop/ui/compose/architecture)
- [Android Developers — Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines)
- [Android Developers — Performance in Jetpack Compose](https://developer.android.com/develop/ui/compose/performance)
- [Android Developers — Testing in Jetpack Compose](https://developer.android.com/develop/ui/compose/testing)
- [Android Developers — Build edge-to-edge apps](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
- [Android Developers — Type safety in Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)
