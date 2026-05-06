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

          {/* 아래 회원목록/로그/API 키 테이블은 기존 그대로 유지 */}
        </div>
      </div>
    </>
  )
}