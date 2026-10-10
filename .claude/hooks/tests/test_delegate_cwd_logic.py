#!/usr/bin/env python3
"""wf-guard-delegate-cwd.sh 的純函式自我檢查。

執行：python3 .claude/hooks/tests/test_delegate_cwd_logic.py
成功印 OK 並以 0 離開；任一 assert 失敗即非零離開。

只涵蓋「會反覆修改且改錯代價最高」的純函式（白名單比對、路徑判定、
差集計算）。端到端情境（hook 實際被 Claude Code 觸發）需偽造整個執行
環境，harness 複雜度遠超被測腳本，走手動驗證清單（見計畫 §5.4）。
"""
import os
import sys
import tempfile

HOOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "wf-guard-delegate-cwd.sh")


def load_logic():
    """從 hook 腳本抽出 python 段落並執行，取得其中定義的純函式。"""
    src = open(HOOK).read()
    start = src.index("# --- LOGIC-START ---")
    end = src.index("# --- LOGIC-END ---")
    ns = {}
    exec(compile(src[start:end], HOOK, "exec"), ns)
    return ns


def test_is_allowed_path():
    ns = load_logic()
    is_allowed = ns["is_allowed"]
    roots = ["/repo/.git", "/home/u/.gradle"]

    assert is_allowed("/repo/.git", roots)
    assert is_allowed("/repo/.git/worktrees/x/HEAD", roots)
    assert is_allowed("/home/u/.gradle/caches/foo.jar", roots)
    # 黑名單：同一顆 repo 內，.git 以外的工作區檔案
    assert not is_allowed("/repo/app/src/main/MainActivity.kt", roots)
    assert not is_allowed("/repo/docs/x.md", roots)
    # 前綴相似但不同目錄，不可誤判為命中
    assert not is_allowed("/repo/.github/workflows/ci.yml", roots)
    assert not is_allowed("/home/u/.gradle-evil/x", roots)


def test_is_allowed_symlink_and_dotdot():
    """is_allowed 兩側都 realpath，故 symlink 與 '..' 應依「解析後」的實際位置判定。

    不測大小寫變體：那取決於檔案系統（APFS 不敏感、ext4 敏感），
    寫死斷言會讓測試在 Linux CI 上假失敗。
    """
    ns = load_logic()
    is_allowed = ns["is_allowed"]

    with tempfile.TemporaryDirectory() as tmp:
        base = os.path.realpath(tmp)
        allowed = os.path.join(base, "repo", ".git")
        outside = os.path.join(base, "repo", "app")
        os.makedirs(allowed, exist_ok=True)
        os.makedirs(outside, exist_ok=True)
        roots = [allowed]

        # symlink：解析後落在白名單內 → 允許；落在外 → 拒絕
        link_in = os.path.join(base, "link_in")
        link_out = os.path.join(base, "link_out")
        os.symlink(allowed, link_in)
        os.symlink(outside, link_out)
        assert is_allowed(link_in, roots)
        assert not is_allowed(link_out, roots)

        # '..'：正規化後回到白名單內 → 允許；跳出去 → 拒絕
        assert is_allowed(os.path.join(allowed, "..", ".git", "config"), roots)
        assert not is_allowed(os.path.join(allowed, "..", "app", "MainActivity.kt"), roots)


def test_whitelist_roots():
    """GRADLE_USER_HOME 必須隔離：呼叫環境若已設，回退路徑的斷言會假失敗。"""
    ns = load_logic()
    old = os.environ.get("GRADLE_USER_HOME")
    try:
        # 未設 → 回退 ~/.gradle，比對確切路徑而非子字串
        os.environ.pop("GRADLE_USER_HOME", None)
        roots = ns["whitelist_roots"]()
        assert isinstance(roots, list) and roots
        assert all(os.path.isabs(r) for r in roots), "白名單必須全為絕對路徑"
        assert os.path.realpath(os.path.expanduser("~/.gradle")) in roots

        # 已設 → 該值須進白名單（realpath 後比對，容忍 symlink）
        with tempfile.TemporaryDirectory() as tmp:
            custom = os.path.realpath(os.path.join(tmp, "gradle-cache"))
            os.makedirs(custom, exist_ok=True)
            os.environ["GRADLE_USER_HOME"] = custom
            assert custom in ns["whitelist_roots"]()
    finally:
        if old is None:
            os.environ.pop("GRADLE_USER_HOME", None)
        else:
            os.environ["GRADLE_USER_HOME"] = old


def test_diff_entries():
    ns = load_logic()
    diff = ns["diff_entries"]
    before = {" M app/A.kt", "?? tmp.txt"}
    after = {" M app/A.kt", "?? tmp.txt", " M app/B.kt"}
    # P-9：只回報新增的，既有 dirty 不算越界
    assert diff(before, after) == {"app/B.kt"}
    assert diff(before, before) == set()
    # 委派前 dirty、委派後變乾淨 → 不是越界
    assert diff(after, before) == set()
    # rename 取新路徑（舊路徑的刪除不是新增的越界寫入）
    assert diff(set(), {"R  a.txt -> b.txt"}) == {"b.txt"}
    # 含空白的檔名，git 會加外層引號
    assert diff(set(), {'?? "un tracked.txt"'}) == {"un tracked.txt"}
    # 非 ASCII：呼叫端帶 core.quotepath=false，故為 UTF-8 原文不需反轉義
    assert diff(set(), {"?? 中文檔名.txt"}) == {"中文檔名.txt"}


if __name__ == "__main__":
    test_is_allowed_path()
    test_is_allowed_symlink_and_dotdot()
    test_whitelist_roots()
    test_diff_entries()
    print("OK")
