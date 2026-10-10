#!/usr/bin/env python3
"""
Sync Android skills, agents, and CLI guidance from https://github.com/android/skills.

Usage:
    python3 sync-android-skills.py [--repo-url URL] [--branch BRANCH] [--dry-run]
"""

import argparse
import filecmp
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


def run_cmd(cmd, cwd=None):
    """Run shell command and return stdout string."""
    result = subprocess.run(
        cmd,
        cwd=cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=True,
    )
    return result.stdout.strip()


def get_git_root():
    """Find the root of the current git repository."""
    try:
        return Path(run_cmd(["git", "rev-parse", "--show-toplevel"]))
    except Exception:
        # Fallback to parent directory containing .claude or .git
        curr = Path(__file__).resolve()
        for parent in [curr] + list(curr.parents):
            if (parent / ".git").exists() or (parent / ".claude").exists():
                return parent
        return Path.cwd()


def file_sha256(filepath):
    """Compute sha256 of a file."""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def dir_diff(src_dir, dst_dir):
    """
    Compare two directories.
    Returns:
        status: 'NEW', 'UPDATED', 'IDENTICAL'
        changed_files: list of relative file paths
    """
    if not dst_dir.exists():
        return "NEW", [str(p.relative_to(src_dir)) for p in src_dir.rglob("*") if p.is_file()]

    src_files = {p.relative_to(src_dir): p for p in src_dir.rglob("*") if p.is_file()}
    dst_files = {p.relative_to(dst_dir): p for p in dst_dir.rglob("*") if p.is_file()}

    changed = []
    # Added or modified
    for rel_path, src_file in src_files.items():
        if rel_path not in dst_files:
            changed.append(str(rel_path))
        elif file_sha256(src_file) != file_sha256(dst_files[rel_path]):
            changed.append(str(rel_path))

    # Deleted in dst
    for rel_path in dst_files:
        if rel_path not in src_files:
            changed.append(f"-{rel_path}")

    if not changed:
        return "IDENTICAL", []
    return "UPDATED", changed


def parse_skill_metadata(skill_md_path):
    """Parse name and description from SKILL.md frontmatter."""
    name = skill_md_path.parent.name
    desc = "No description provided."
    if not skill_md_path.exists():
        return name, desc

    try:
        content = skill_md_path.read_text(encoding="utf-8")
        if content.startswith("---"):
            parts = content.split("---", 2)
            if len(parts) >= 3:
                frontmatter = parts[1]
                name_match = re.search(r"^name:\s*(.+)$", frontmatter, re.MULTILINE)
                if name_match:
                    name = name_match.group(1).strip().strip("'\"")

                desc_match = re.search(r"^description:\s*(.+?)(?=\n[a-zA-Z0-9_-]+:|\Z)", frontmatter, re.DOTALL | re.MULTILINE)
                if desc_match:
                    # Clean up multi-line YAML scalar
                    raw_desc = desc_match.group(1).strip()
                    desc = " ".join(line.strip() for line in raw_desc.splitlines()).strip("'\"")
    except Exception as e:
        print(f"Warning: Failed to parse metadata from {skill_md_path}: {e}")

    return name, desc


SKILL_ZH_TW_DESCRIPTIONS = {
    "adaptive": "Jetpack Compose 自適應排版指南，針對不同螢幕尺寸、折疊狀態及多視窗環境調整 UI 佈局。",
    "agp-9-upgrade": "升級與遷移 Android 專案至 Android Gradle Plugin (AGP) 9，處理建構設定與相容性。",
    "android-cli": "官方 android 命令列工具操作手冊，包含專案建置、裝置互動除錯、虛擬機管理與官方文檔檢索。",
    "android-intent-security": "Android Intent 安全最佳實踐，審查組件匯出設定與輸入驗證，防範 Intent 重定向與未授權存取漏洞。",
    "android-permissions-security": "權限與 IPC 安全性審查，治理執行期權限申請流程、自訂權限與跨行程 Binder 呼叫驗證。",
    "android-profiler": "Android Studio 效能剖析與除錯指引，涵蓋 CPU、記憶體、電量監控、Macrobenchmark 與啟動時間優化。",
    "appfunctions": "裝置端 AI 與系統 Agent 專用功能分析，定義、實作並優化 AppFunctions 與 KDoc 工具描述。",
    "camerax": "現代 CameraX 相機架構開發，處理非同步錄影生命週期、硬體相容性以及 ML Kit / Media3 特效整合。",
    "display-glasses-with-jetpack-compose-glimmer": "智慧顯示眼鏡 (XR) 投影應用開發指引，使用 Jetpack Compose 與 Glimmer 打造極簡穿戴視覺介面。",
    "edge-to-edge": "Android 15 (API 35+) 全螢幕邊到邊 (Edge-to-Edge) 適配，處理 WindowInsets 系統列與鍵盤邊界重疊。",
    "engage-sdk-integration": "Google Play Core Engage SDK 整合，將 App 內容叢集映射發布至系統動態推薦介面。",
    "leanback-to-compose-tv-migration": "Android TV 遷移指引，將過時的 Leanback 架構升級遷移至現代 Jetpack Compose for TV。",
    "media3-cast-integration": "基於 Jetpack Media3 的 Google Cast 投放整合，配置 CastPlayer 與 Compose 遠端播放架構。",
    "migrate-xml-views-to-jetpack-compose": "傳統 XML Views 漸進式遷移至 Jetpack Compose 的標準工作流程，涵蓋主題轉換與雙向互通。",
    "ml-kit-genai-prompt-api": "裝置端 Gemini Nano 與 ML Kit GenAI Prompt API 整合，實現結構化輸出與前綴快取優化。",
    "navigation-3": "最新世代 Jetpack Navigation 3 架構，支援場景路由、多返回堆疊、深層連結與型別安全導航。",
    "navigation-event": "導航事件與預測性返回手勢 (Predictive Back) 動畫攔截，規範單次導航行為與生命週期治理。",
    "play-billing-library-version-upgrade": "Google Play 結帳函式庫 (Play Billing Library) 最新版本升級與遷移最佳實踐。",
    "play-policy-insights": "Google Play 開發者政策合規性自動審查，比對靜態代碼與商店宣告（資料安全、權限與帳號衛生）。",
    "r8-analyzer": "R8 與 ProGuard 混淆規則分析器，識別並消除冗餘、過寬的 Keep Rules 以最佳化 App 體積。",
    "restore-credentials": "Android Credential Manager 還原憑證整合，提供無縫且自動化的換機無感登入流程。",
    "styles": "Jetpack Compose Styles API 整合，建立可樣式化自訂元件與統一的互動狀態修飾。",
    "testing-setup": "Android 測試基礎設施建置，涵蓋單元測試、Flow Turbine 測試、Compose UI 語義測試與截圖測試。",
    "verified-email": "Credential Manager 驗證信箱 API 整合，實現免簡訊 OTP、高轉換率的安全電子郵件驗證。",
    "wear-compose-m3": "Wear OS 智慧手錶應用開發指南，採用 Compose Material 3 打造穿戴裝置專用 UI 與環境模式。",
}


def get_skill_zh_desc(name, default_desc):
    """Return traditional Chinese description if available, otherwise fallback."""
    return SKILL_ZH_TW_DESCRIPTIONS.get(name, default_desc)


def update_android_expert_agent(project_root, detected_skills, dry_run=False):
    """
    Update .claude/agents/android-expert.md and .agents/agents/android-expert.yaml
    with the current list of skills using Traditional Chinese descriptions.
    """
    skills_bullet = "\n".join(
        f"  - `{s['name']}`: {get_skill_zh_desc(s['name'], s['desc'])}"
        for s in detected_skills
    )

    claude_agent_path = project_root / ".claude" / "agents" / "android-expert.md"
    claude_content = f"""---
name: android-expert
description: 現代 Android 與 Jetpack Compose 專家，整合 Google 官方 Android Skills、Clean Architecture、Kotlin 2.0+ 與 API 35+ 規範。
model: sonnet
---

你是 android_expert 專業型 subagent profile。

職責與專長：
- 現代 Android 架構（Guide to App Architecture、Clean Architecture、UDF、MVI/MVVM）。
- Jetpack Compose、Material 3、Adaptive UI 與 Edge-to-Edge（Android 15 / API 35+）。
- 嚴格遵守專案 `.claude/rules/android-styles.md`、Effective Android 與 Effective Kotlin 準則。
- 調度與應用 Google 官方 Android Skills（共 {len(detected_skills)} 項）：
{skills_bullet}

執行模式：
- 先分析 `build.gradle.kts` 與 `libs.versions.toml`，避免無謂引入不相容或重複依賴。
- 遵循不可變性優先（Immutability by default），消滅雙驚嘆號（`!!`），優先採用 `Result<T>` 與 `sealed interface`。
- ViewModel 串流採用 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ...)`，UI 收集一律使用 `collectAsStateWithLifecycle()`。
- 遵循單向資料流（UDF）與狀態提升（State Hoisting）。

允許寫入：
- Android 原始碼檔案（`app/src/main/java/*`, `app/src/main/res/*`）
- 測試檔案（`app/src/test/*`, `app/src/androidTest/*`）
- 建構設定檔（`build.gradle.kts`, `gradle/libs.versions.toml`）
- 相關規格與架構文件

禁止寫入：
- 無關的非 Android 檔案
- 未經授權變更專案整體架構邊界

完成前：
- 摘要所有變更並確認代碼符合 `.claude/rules/android-styles.md` 規範。
"""

    yaml_agent_path = project_root / ".agents" / "agents" / "android-expert.yaml"
    # Indent for YAML multi-line string
    indented_bullet = "\n".join(
        f"    - `{s['name']}`: {get_skill_zh_desc(s['name'], s['desc'])}"
        for s in detected_skills
    )
    yaml_content = f"""name: android-expert
description: 現代 Android 與 Jetpack Compose 專家，整合 Google 官方 Android Skills、Clean Architecture、Kotlin 2.0+ 與 API 35+ 規範。
enable_write_tools: true
enable_mcp_tools: true
enable_subagent_tools: false
system_prompt: |
  你是 android_expert subagent。

  職責與專長：
  - 現代 Android 架構（Guide to App Architecture、Clean Architecture、UDF、MVI/MVVM）。
  - Jetpack Compose、Material 3、Adaptive UI 與 Edge-to-Edge（Android 15 / API 35+）。
  - 嚴格遵守專案 `.claude/rules/android-styles.md`、Effective Android 與 Effective Kotlin 準則。
  - 調度與應用 Google 官方 Android Skills（共 {len(detected_skills)} 項）：
{indented_bullet}

  執行模式：
  - 先分析 `build.gradle.kts` 與 `libs.versions.toml`，避免無謂引入不相容或重複依賴。
  - 遵循不可變性優先（Immutability by default），消滅雙驚嘆號（`!!`），優先採用 `Result<T>` 與 `sealed interface`。
  - ViewModel 串流採用 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ...)`，UI 收集一律使用 `collectAsStateWithLifecycle()`。
  - 遵循單向資料流（UDF）與狀態提升（State Hoisting）。

  允許寫入：
  - Android 原始碼檔案（`app/src/main/java/*`, `app/src/main/res/*`）
  - 測試檔案（`app/src/test/*`, `app/src/androidTest/*`）
  - 建構設定檔（`build.gradle.kts`, `gradle/libs.versions.toml`）
  - 相關規格與架構文件

  禁止寫入：
  - 無關的非 Android 檔案
  - 未經授權變更專案整體架構邊界

  完成前：
  - 摘要所有變更並確認代碼符合 `.claude/rules/android-styles.md` 規範。
"""

    agent_status = "UNCHANGED"
    if not dry_run:
        claude_agent_path.parent.mkdir(parents=True, exist_ok=True)
        old_claude = claude_agent_path.read_text(encoding="utf-8") if claude_agent_path.exists() else ""
        if old_claude != claude_content:
            claude_agent_path.write_text(claude_content, encoding="utf-8")
            agent_status = "UPDATED"

        yaml_agent_path.parent.mkdir(parents=True, exist_ok=True)
        old_yaml = yaml_agent_path.read_text(encoding="utf-8") if yaml_agent_path.exists() else ""
        if old_yaml != yaml_content:
            yaml_agent_path.write_text(yaml_content, encoding="utf-8")
            agent_status = "UPDATED"

    return agent_status


def main():
    parser = argparse.ArgumentParser(description="Sync Android skills and agents from GitHub upstream.")
    parser.add_argument("--repo-url", default="https://github.com/android/skills", help="Git repo URL")
    parser.add_argument("--branch", default="main", help="Git branch or tag to sync")
    parser.add_argument("--dry-run", action="store_true", help="Perform trial run without changing files")
    parser.add_argument("--project-root", default=None, help="Root path of the project")
    args = parser.parse_args()

    project_root = Path(args.project_root).resolve() if args.project_root else get_git_root()
    claude_skills_dir = project_root / ".claude" / "skills"
    agents_skills_dir = project_root / ".agents" / "skills"

    print(f"============================================================")
    print(f"  Android Skills & Agents Synchronizer")
    print(f"============================================================")
    print(f"Target Project Root: {project_root}")
    print(f"Upstream Repository: {args.repo_url} (branch: {args.branch})")
    print(f"Dry Run Mode:        {args.dry_run}")
    print(f"------------------------------------------------------------")

    with tempfile.TemporaryDirectory(prefix="android_skills_sync_") as temp_dir:
        temp_path = Path(temp_dir)
        print(f"Cloning upstream repository...")
        run_cmd(["git", "clone", "--depth", "1", "-b", args.branch, args.repo_url, str(temp_path)])

        commit_hash = run_cmd(["git", "rev-parse", "HEAD"], cwd=temp_path)
        commit_date = run_cmd(["git", "log", "-1", "--format=%cd", "--date=short"], cwd=temp_path)
        commit_msg = run_cmd(["git", "log", "-1", "--pretty=%s"], cwd=temp_path)
        print(f"Upstream HEAD: {commit_hash[:8]} ({commit_date}) - {commit_msg}")
        print(f"------------------------------------------------------------")

        # 1. Discover all skills
        # Search for all directories containing SKILL.md
        found_skills = []
        for skill_file in temp_path.rglob("SKILL.md"):
            skill_dir = skill_file.parent
            # Avoid root or hidden directories
            rel_to_temp = skill_dir.relative_to(temp_path)
            if any(part.startswith(".") for part in rel_to_temp.parts):
                continue

            name, desc = parse_skill_metadata(skill_file)
            found_skills.append({
                "name": name,
                "desc": desc,
                "src_dir": skill_dir,
                "rel_path": rel_to_temp,
            })

        # Sort by name
        found_skills.sort(key=lambda s: s["name"])
        print(f"Found {len(found_skills)} skills in upstream repository.\n")

        # 2. Sync skills to .claude/skills/<name>
        results = {"NEW": [], "UPDATED": [], "IDENTICAL": []}
        claude_skills_dir.mkdir(parents=True, exist_ok=True)

        for s in found_skills:
            target_skill_dir = claude_skills_dir / s["name"]
            status, diff = dir_diff(s["src_dir"], target_skill_dir)
            results[status].append(s["name"])

            if not args.dry_run:
                # Copy or update
                if target_skill_dir.exists():
                    shutil.rmtree(target_skill_dir)
                shutil.copytree(s["src_dir"], target_skill_dir)

                # Ensure scripts are executable
                scripts_dir = target_skill_dir / "scripts"
                if scripts_dir.exists():
                    for script in scripts_dir.glob("*"):
                        if script.suffix in [".sh", ".py", ".ps1"]:
                            script.chmod(script.stat().st_mode | 0o111)

            diff_info = f" ({len(diff)} changes)" if diff and status == "UPDATED" else ""
            indicator = "[+] NEW" if status == "NEW" else ("[*] UPDATED" if status == "UPDATED" else "[-] UP-TO-DATE")
            print(f"{indicator:14} {s['name']:<40}{diff_info}")

        # 3. Sync LICENSE.txt
        license_src = temp_path / "LICENSE.txt"
        license_dst = claude_skills_dir / "ANDROID_SKILLS_LICENSE.txt"
        license_status = "SKIPPED"
        if license_src.exists():
            if not license_dst.exists() or file_sha256(license_src) != file_sha256(license_dst):
                license_status = "UPDATED"
                if not args.dry_run:
                    shutil.copy2(license_src, license_dst)
            else:
                license_status = "UP-TO-DATE"
        print(f"\nLicense Status: {license_status} -> {license_dst.name}")

        # 4. Check for any upstream agent definitions
        upstream_agents_found = 0
        for agent_file in list(temp_path.rglob("*.yaml")) + list(temp_path.rglob("*.md")):
            rel_parts = agent_file.relative_to(temp_path).parts
            if "agents" in rel_parts and not any(p.startswith(".") for p in rel_parts):
                print(f"Upstream agent found: {agent_file.relative_to(temp_path)}")
                upstream_agents_found += 1
                if not args.dry_run:
                    dst = project_root / ".claude" / "agents" / agent_file.name
                    shutil.copy2(agent_file, dst)

        # 5. Update android-expert agent to bind all detected skills
        agent_sync_status = update_android_expert_agent(project_root, found_skills, dry_run=args.dry_run)
        print(f"Android Expert Agent: {agent_sync_status} (bound {len(found_skills)} skills)")

        # 6. Verify symlink
        symlink_ok = agents_skills_dir.is_symlink() and agents_skills_dir.resolve() == claude_skills_dir.resolve()
        print(f"Symlink .agents/skills -> .claude/skills: {'VALID' if symlink_ok else 'MISSING/INVALID'}")

        print(f"\n============================================================")
        print(f"  Summary:")
        print(f"  - New Skills:      {len(results['NEW'])}")
        print(f"  - Updated Skills:  {len(results['UPDATED'])}")
        print(f"  - Unchanged:       {len(results['IDENTICAL'])}")
        print(f"  - Total Skills:    {len(found_skills)}")
        print(f"============================================================")

    return 0


if __name__ == "__main__":
    sys.exit(main())
