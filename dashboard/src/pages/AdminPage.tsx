import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import { useEffect, useMemo, useState } from "react"
import {
  Shield,
  Users,
  KeyRound,
  Activity,
  RefreshCw,
  Search,
  Download,
} from "lucide-react"

type AdminPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

type AdminUserItem = {
  id: number
  email: string
  role: string
  grade: string
  status: string
  provider: string
  createdAt: string | null
  lastLoginAt: string | null
}

type AdminLoginLogItem = {
  id: number
  userId: number | null
  email: string
  success: boolean
  provider: string
  loginAt: string | null
  message: string | null
}

type AdminApiKeyItem = {
  id: number
  userId: number
  apiKey: string
  status: string
  createdAt: string | null
  revokedAt: string | null
}

type UsersResponse = AdminUserItem[]
type LoginLogsResponse = AdminLoginLogItem[]
type ApiKeysResponse = AdminApiKeyItem[]

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "https://api.autohwpx.com"

function formatDateTime(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date)
}

function maskApiKey(value?: string | null) {
  if (!value) return "-"
  if (value.length <= 12) return value
  return `${value.slice(0, 8)}••••••••${value.slice(-6)}`
}

function providerLabel(provider?: string | null) {
  const value = (provider ?? "local").toLowerCase()

  if (value === "google") return "Google"
  if (value === "kakao") return "Kakao"
  if (value === "naver") return "Naver"
  if (value === "local") return "Local"

  return value
}

export default function AdminPage({ onNavigate, onLogout }: AdminPageProps) {
  const [users, setUsers] = useState<AdminUserItem[]>([])
  const [loginLogs, setLoginLogs] = useState<AdminLoginLogItem[]>([])
  const [apiKeys, setApiKeys] = useState<AdminApiKeyItem[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState("")
  const [keyword, setKeyword] = useState("")
  const [backupDownloading, setBackupDownloading] = useState(false)

  useEffect(() => {
    const adminToken = localStorage.getItem("adminToken") ?? ""
    if (!adminToken) {
      setLoading(false)
      setError("관리자 토큰이 없습니다. 먼저 관리자 로그인해주세요.")
      return
    }

    const controller = new AbortController()

    async function fetchAdminData() {
      try {
        setLoading(true)
        setError("")

        const [usersRes, logsRes, keysRes] = await Promise.all([
          fetch(`${API_BASE_URL}/api/v1/admin/users`, {
            method: "GET",
            headers: {
              "X-Admin-Token": adminToken,
            },
            credentials: "include",
            signal: controller.signal,
          }),
          fetch(`${API_BASE_URL}/api/v1/admin/login-logs`, {
            method: "GET",
            headers: {
              "X-Admin-Token": adminToken,
            },
            credentials: "include",
            signal: controller.signal,
          }),
          fetch(`${API_BASE_URL}/api/v1/admin/api-keys`, {
            method: "GET",
            headers: {
              "X-Admin-Token": adminToken,
            },
            credentials: "include",
            signal: controller.signal,
          }),
        ])

        if (!usersRes.ok) throw new Error("유저 목록 조회 실패")
        if (!logsRes.ok) throw new Error("로그인 로그 조회 실패")
        if (!keysRes.ok) throw new Error("API 키 목록 조회 실패")

        const usersData: UsersResponse = await usersRes.json()
        const logsData: LoginLogsResponse = await logsRes.json()
        const keysData: ApiKeysResponse = await keysRes.json()

        setUsers(usersData ?? [])
        setLoginLogs(logsData ?? [])
        setApiKeys(keysData ?? [])
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") return
        console.error(err)
        setError("관리자 데이터를 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchAdminData()

    return () => controller.abort()
  }, [])

  async function reloadAdminData() {
    const adminToken = localStorage.getItem("adminToken") ?? ""
    if (!adminToken) return

    try {
      setRefreshing(true)

      const [usersRes, logsRes, keysRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/v1/admin/users`, {
          method: "GET",
          headers: { "X-Admin-Token": adminToken },
          credentials: "include",
        }),
        fetch(`${API_BASE_URL}/api/v1/admin/login-logs`, {
          method: "GET",
          headers: { "X-Admin-Token": adminToken },
          credentials: "include",
        }),
        fetch(`${API_BASE_URL}/api/v1/admin/api-keys`, {
          method: "GET",
          headers: { "X-Admin-Token": adminToken },
          credentials: "include",
        }),
      ])

      if (!usersRes.ok) throw new Error("유저 목록 조회 실패")
      if (!logsRes.ok) throw new Error("로그인 로그 조회 실패")
      if (!keysRes.ok) throw new Error("API 키 목록 조회 실패")

      setUsers((await usersRes.json()) ?? [])
      setLoginLogs((await logsRes.json()) ?? [])
      setApiKeys((await keysRes.json()) ?? [])
    } catch (err) {
      console.error(err)
      alert("관리자 데이터 새로고침에 실패했습니다.")
    } finally {
      setRefreshing(false)
    }
  }

  async function handleDebugDbPath() {
    const adminToken = localStorage.getItem("adminToken") ?? ""

    if (!adminToken) {
      alert("관리자 토큰이 없습니다.")
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/admin/debug/db-path`, {
        method: "GET",
        headers: {
          "X-Admin-Token": adminToken,
        },
        credentials: "include",
      })

      const data = await response.json()

      alert(JSON.stringify(data, null, 2))
    } catch (err) {
      console.error(err)
      alert("DB 경로 확인 실패")
    }
  }

  async function handleDownloadDbBackup() {
    const adminToken = localStorage.getItem("adminToken") ?? ""

    if (!adminToken) {
      alert("관리자 토큰이 없습니다.")
      return
    }

    try {
      setBackupDownloading(true)

      const response = await fetch(`${API_BASE_URL}/api/v1/admin/backup/db`, {
        method: "GET",
        headers: {
          "X-Admin-Token": adminToken,
        },
        credentials: "include",
      })

      if (!response.ok) {
        const text = await response.text()
        throw new Error(`DB 백업 다운로드 실패 (${response.status}): ${text}`)
      }

      const blob = await response.blob()

      const contentDisposition = response.headers.get("Content-Disposition")
      const filenameMatch = contentDisposition?.match(/filename="?([^"]+)"?/)
      const filename = filenameMatch?.[1] ?? `hwpx-backup-${Date.now()}.db`

      const url = window.URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "DB 백업 다운로드 중 오류가 발생했습니다.")
    } finally {
      setBackupDownloading(false)
    }
  }


  async function handleChangeGrade(userId: number, grade: string) {
    const adminToken = localStorage.getItem("adminToken") ?? ""
    if (!adminToken) {
      alert("관리자 토큰이 없습니다.")
      return
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/admin/users/${userId}/grade`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            "X-Admin-Token": adminToken,
          },
          body: JSON.stringify({ grade }),
        }
      )

      const data = await response.json()

      if (!response.ok || !data.ok) {
        throw new Error(data.message ?? "등급 변경 실패")
      }

      setUsers((prev) =>
        prev.map((user) =>
          user.id === userId ? { ...user, grade } : user
        )
      )
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "등급 변경에 실패했습니다.")
    }
  }

  async function handleBlockUser(userId: number) {
    const adminToken = localStorage.getItem("adminToken") ?? ""
    if (!adminToken) {
      alert("관리자 토큰이 없습니다.")
      return
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/admin/users/${userId}/block`,
        {
          method: "PATCH",
          headers: {
            "X-Admin-Token": adminToken,
          },
        }
      )

      const data = await response.json()

      if (!response.ok || !data.ok) {
        throw new Error(data.message ?? "유저 차단 실패")
      }

      setUsers((prev) =>
        prev.map((user) =>
          user.id === userId ? { ...user, status: "BLOCKED" } : user
        )
      )
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "유저 차단에 실패했습니다.")
    }
  }

  async function handleUnblockUser(userId: number) {
    const adminToken = localStorage.getItem("adminToken") ?? ""
    if (!adminToken) {
      alert("관리자 토큰이 없습니다.")
      return
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/admin/users/${userId}/unblock`,
        {
          method: "PATCH",
          headers: {
            "X-Admin-Token": adminToken,
          },
        }
      )

      const data = await response.json()

      if (!response.ok || !data.ok) {
        throw new Error(data.message ?? "유저 차단 해제 실패")
      }

      setUsers((prev) =>
        prev.map((user) =>
          user.id === userId ? { ...user, status: "ACTIVE" } : user
        )
      )
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "유저 차단 해제에 실패했습니다.")
    }
  }

  async function handleDeleteUser(userId: number) {
    const adminToken = localStorage.getItem("adminToken") ?? ""
    if (!adminToken) {
      alert("관리자 토큰이 없습니다.")
      return
    }

    const ok = window.confirm("정말 이 유저를 삭제 처리할까요?")
    if (!ok) return

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/admin/users/${userId}`, {
        method: "DELETE",
        headers: {
          "X-Admin-Token": adminToken,
        },
        credentials: "include",
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        throw new Error(data.message ?? "유저 삭제 실패")
      }

      setUsers((prev) =>
        prev.map((user) =>
          user.id === userId ? { ...user, status: "DELETED" } : user
        )
      )
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "유저 삭제에 실패했습니다.")
    }
  }

  const filteredUsers = useMemo(() => {
    if (!keyword.trim()) return users
    const q = keyword.toLowerCase()
    return users.filter(
      (item) =>
        item.email.toLowerCase().includes(q) ||
        item.role.toLowerCase().includes(q) ||
        item.grade.toLowerCase().includes(q) ||
        item.status.toLowerCase().includes(q) ||
        providerLabel(item.provider).toLowerCase().includes(q)
    )
  }, [users, keyword])

  const filteredLogs = useMemo(() => {
    if (!keyword.trim()) return loginLogs
    const q = keyword.toLowerCase()
    return loginLogs.filter(
      (item) =>
        item.email.toLowerCase().includes(q) ||
        providerLabel(item.provider).toLowerCase().includes(q) ||
        (item.message ?? "").toLowerCase().includes(q)
    )
  }, [loginLogs, keyword])

  const filteredApiKeys = useMemo(() => {
    if (!keyword.trim()) return apiKeys
    const q = keyword.toLowerCase()
    return apiKeys.filter(
      (item) =>
        item.apiKey.toLowerCase().includes(q) ||
        item.status.toLowerCase().includes(q) ||
        String(item.userId).includes(q)
    )
  }, [apiKeys, keyword])

  const activeUsersCount = users.filter((item) => item.status === "ACTIVE").length
  const successLoginsCount = loginLogs.filter((item) => item.success).length
  const activeKeysCount = apiKeys.filter((item) => item.status === "ACTIVE").length

  if (loading) {
    return (
      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6">
            <p className="text-lg font-semibold text-[#1a2540]">관리자 페이지 불러오는 중...</p>
            <p className="mt-2 text-sm text-[#63708a]">
              유저, 로그인 로그, API 키 정보를 확인하고 있어요.
            </p>
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[32px] border border-red-200 bg-white p-6">
            <p className="text-lg font-semibold text-red-600">에러</p>
            <p className="mt-2 text-sm text-slate-600">{error}</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage={"admin" as PageType} onLogout={onLogout} />

      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 rounded-[32px] border border-[#cfe0ff] bg-white/85 p-6 backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                  <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
                  AutoHWPX
                </div>
                <h1 className="text-3xl font-bold tracking-tight text-[#162033] sm:text-4xl">
                  관리자 페이지
                </h1>
                <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                  회원, 로그인 이력, API 키 상태를 관리하는 공간
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <div className="flex items-center gap-2 rounded-2xl border border-[#d7e5ff] bg-[#f3f8ff] px-4 py-3 text-sm">
                  <Search className="h-4 w-4 text-[#7c8aa5]" />
                  <input
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    placeholder="이메일, provider, 등급, 상태 검색"
                    className="w-full bg-transparent outline-none placeholder:text-[#9aa7bf] sm:w-64"
                  />
                </div>

                <button
                  onClick={handleDownloadDbBackup}
                  disabled={backupDownloading}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3 text-sm font-semibold text-[#1f69ef] hover:bg-[#f5f9ff] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Download className={`h-4 w-4 ${backupDownloading ? "animate-bounce" : ""}`} />
                  {backupDownloading ? "백업 다운로드 중..." : "DB 백업 다운로드"}
                </button>

                <button
                  onClick={handleDebugDbPath}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3 text-sm font-semibold text-[#1f69ef]"
                >
                  DB 경로 확인
                </button>

                <button
                  onClick={reloadAdminData}
                  disabled={refreshing}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl bg-[#1f69ef] px-4 py-3 text-sm font-semibold text-white hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <RefreshCw className={`h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
                  새로고침
                </button>
              </div>
            </div>
          </header>

          <section className="mb-6 grid gap-4 sm:grid-cols-3">
            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <div className="flex items-center gap-2 text-sm text-slate-500">
                  <Users className="h-4 w-4 text-[#1f69ef]" />
                  전체 회원
                </div>
                <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                  {users.length}
                </p>
                <p className="mt-1 text-xs text-[#63708a]">활성 회원 {activeUsersCount}명</p>
              </div>
            </div>

            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <div className="flex items-center gap-2 text-sm text-slate-500">
                  <Activity className="h-4 w-4 text-[#1f69ef]" />
                  로그인 기록
                </div>
                <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                  {loginLogs.length}
                </p>
                <p className="mt-1 text-xs text-[#63708a]">성공 로그인 {successLoginsCount}건</p>
              </div>
            </div>

            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <div className="flex items-center gap-2 text-sm text-slate-500">
                  <KeyRound className="h-4 w-4 text-[#1f69ef]" />
                  API 키
                </div>
                <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                  {apiKeys.length}
                </p>
                <p className="mt-1 text-xs text-[#63708a]">활성 키 {activeKeysCount}개</p>
              </div>
            </div>
          </section>

          <div className="space-y-6">
            <section className="rounded-[32px] border border-[#dbe7ff] bg-white">
              <div className="flex items-center justify-between gap-4 p-6 pb-0">
                <div>
                  <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                    <Users className="h-5 w-5 text-[#1f69ef]" />
                    회원 목록
                  </h2>
                  <p className="text-sm text-[#63708a]">가입 회원과 현재 등급 상태</p>
                </div>
              </div>

              <div className="p-6">
                <div className="overflow-hidden rounded-[24px] border border-[#dbe7ff]">
                  <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                      <thead className="bg-[#f3f8ff] text-[#45608d]">
                        <tr>
                          <th className="px-4 py-3 text-center font-semibold">ID</th>
                          <th className="px-4 py-3 text-center font-semibold">이메일</th>
                          <th className="px-4 py-3 text-center font-semibold">Provider</th>
                          <th className="px-4 py-3 text-center font-semibold">등급</th>
                          <th className="px-4 py-3 text-center font-semibold">권한</th>
                          <th className="px-4 py-3 text-center font-semibold">상태</th>
                          <th className="px-4 py-3 text-center font-semibold">가입일</th>
                          <th className="px-4 py-3 text-center font-semibold">최근 로그인</th>
                          <th className="px-4 py-3 text-center font-semibold">등급 변경</th>
                          <th className="px-4 py-3 text-center font-semibold">관리</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredUsers.length === 0 ? (
                          <tr className="border-t border-[#dbe7ff] bg-white">
                            <td colSpan={10} className="px-4 py-8 text-center text-[#63708a]">
                              표시할 회원이 없습니다.
                            </td>
                          </tr>
                        ) : (
                          filteredUsers.map((item) => (
                            <tr key={item.id} className="border-t border-[#dbe7ff] bg-white">
                              <td className="px-4 py-3 text-center text-[#1a2540]">{item.id}</td>
                              <td className="px-4 py-3 text-center text-[#1a2540]">{item.email}</td>
                              <td className="px-4 py-3 text-center">
                                <span className="rounded-full bg-[#eef4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                                  {providerLabel(item.provider)}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center">
                                <span className="rounded-full bg-[#eef4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                                  {item.grade}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">{item.role}</td>
                              <td className="px-4 py-3 text-center">
                                <span
                                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                    item.status === "ACTIVE"
                                      ? "bg-[#eef4ff] text-[#1f69ef]"
                                      : item.status === "BLOCKED"
                                      ? "bg-amber-50 text-amber-700"
                                      : "bg-red-50 text-red-700"
                                  }`}
                                >
                                  {item.status === "ACTIVE"
                                    ? "ACTIVE"
                                    : item.status === "BLOCKED"
                                    ? "BLOCKED"
                                    : "DELETED"}                                  
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">
                                {formatDateTime(item.createdAt)}
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">
                                {formatDateTime(item.lastLoginAt)}
                              </td>

                              <td className="px-4 py-3 text-center">
                                <select
                                  value={item.grade}
                                  onChange={(e) => handleChangeGrade(item.id, e.target.value)}
                                  className="rounded-xl border border-[#d7e5ff] bg-white px-3 py-2 text-sm text-[#1a2540] outline-none"
                                  disabled={item.status === "DELETED"}
                                >
                                  <option value="STARTER">STARTER</option>
                                  <option value="PRO">PRO</option>
                                  <option value="BUSINESS">BUSINESS</option>
                                </select>
                              </td>

                              <td className="px-4 py-3 text-center">
                                <div className="flex justify-center gap-2">
                                  {item.status === "BLOCKED" ? (
                                    <button
                                      onClick={() => handleUnblockUser(item.id)}
                                      className="rounded-xl bg-[#1f69ef] px-3 py-2 text-xs font-semibold text-white hover:bg-[#1859ca]"
                                    >
                                      차단 해제
                                    </button>
                                  ) : (
                                    <button
                                      onClick={() => handleBlockUser(item.id)}
                                      disabled={item.status === "DELETED"}
                                      className="rounded-xl bg-amber-500 px-3 py-2 text-xs font-semibold text-white hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                      차단
                                    </button>
                                  )}

                                  <button
                                    onClick={() => handleDeleteUser(item.id)}
                                    disabled={item.status === "DELETED"}
                                    className="rounded-xl bg-red-500 px-3 py-2 text-xs font-semibold text-white hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                                  >
                                    삭제
                                  </button>
                                </div>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </section>

            <section className="rounded-[32px] border border-[#dbe7ff] bg-white">
              <div className="flex items-center justify-between gap-4 p-6 pb-0">
                <div>
                  <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                    <Activity className="h-5 w-5 text-[#1f69ef]" />
                    로그인 로그
                  </h2>
                  <p className="text-sm text-[#63708a]">로그인 성공 / 실패 이력</p>
                </div>
              </div>

              <div className="p-6">
                <div className="overflow-hidden rounded-[24px] border border-[#dbe7ff]">
                  <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                      <thead className="bg-[#f3f8ff] text-[#45608d]">
                        <tr>
                          <th className="px-4 py-3 text-center font-semibold">ID</th>
                          <th className="px-4 py-3 text-center font-semibold">이메일</th>
                          <th className="px-4 py-3 text-center font-semibold">Provider</th>
                          <th className="px-4 py-3 text-center font-semibold">결과</th>
                          <th className="px-4 py-3 text-center font-semibold">메시지</th>
                          <th className="px-4 py-3 text-center font-semibold">로그인 시각</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredLogs.length === 0 ? (
                          <tr className="border-t border-[#dbe7ff] bg-white">
                            <td colSpan={6} className="px-4 py-8 text-center text-[#63708a]">
                              표시할 로그인 로그가 없습니다.
                            </td>
                          </tr>
                        ) : (
                          filteredLogs.map((item) => (
                            <tr key={item.id} className="border-t border-[#dbe7ff] bg-white">
                              <td className="px-4 py-3 text-center text-[#1a2540]">{item.id}</td>
                              <td className="px-4 py-3 text-center text-[#1a2540]">{item.email}</td>
                              <td className="px-4 py-3 text-center">
                                <span className="rounded-full bg-[#eef4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                                  {providerLabel(item.provider)}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center">
                                <span
                                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                    item.success
                                      ? "bg-[#eef4ff] text-[#1f69ef]"
                                      : "bg-red-50 text-red-700"
                                  }`}
                                >
                                  {item.success ? "성공" : "실패"}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">
                                {item.message || "-"}
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">
                                {formatDateTime(item.loginAt)}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </section>

            <section className="rounded-[32px] border border-[#dbe7ff] bg-white">
              <div className="flex items-center justify-between gap-4 p-6 pb-0">
                <div>
                  <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                    <Shield className="h-5 w-5 text-[#1f69ef]" />
                    API 키 목록
                  </h2>
                  <p className="text-sm text-[#63708a]">발급된 키와 현재 상태</p>
                </div>
              </div>

              <div className="p-6">
                <div className="overflow-hidden rounded-[24px] border border-[#dbe7ff]">
                  <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                      <thead className="bg-[#f3f8ff] text-[#45608d]">
                        <tr>
                          <th className="px-4 py-3 text-center font-semibold">ID</th>
                          <th className="px-4 py-3 text-center font-semibold">유저 ID</th>
                          <th className="px-4 py-3 text-center font-semibold">API 키</th>
                          <th className="px-4 py-3 text-center font-semibold">상태</th>
                          <th className="px-4 py-3 text-center font-semibold">발급일</th>
                          <th className="px-4 py-3 text-center font-semibold">폐기일</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredApiKeys.length === 0 ? (
                          <tr className="border-t border-[#dbe7ff] bg-white">
                            <td colSpan={6} className="px-4 py-8 text-center text-[#63708a]">
                              표시할 API 키가 없습니다.
                            </td>
                          </tr>
                        ) : (
                          filteredApiKeys.map((item) => (
                            <tr key={item.id} className="border-t border-[#dbe7ff] bg-white">
                              <td className="px-4 py-3 text-center text-[#1a2540]">{item.id}</td>
                              <td className="px-4 py-3 text-center text-[#1a2540]">{item.userId}</td>
                              <td className="px-4 py-3 text-center font-mono text-[#63708a]">
                                {maskApiKey(item.apiKey)}
                              </td>
                              <td className="px-4 py-3 text-center">
                                <span
                                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                    item.status === "ACTIVE"
                                      ? "bg-[#eef4ff] text-[#1f69ef]"
                                      : "bg-red-50 text-red-700"
                                  }`}
                                >
                                  {item.status}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">
                                {formatDateTime(item.createdAt)}
                              </td>
                              <td className="px-4 py-3 text-center text-[#63708a]">
                                {formatDateTime(item.revokedAt)}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </>
  )
}