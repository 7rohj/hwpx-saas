import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import { useEffect, useMemo, useState } from "react"
import {
  Activity,
  CheckCircle2,
  XCircle,
  Search,
  Filter,
  RefreshCw,
} from "lucide-react"

type UsageLogItem = {
  id: number
  actionType: string
  targetId: string | null
  success: boolean
  message: string | null
  createdAt: string
}

type UsageLogsResponse = {
  ok: boolean
  count: number
  items: UsageLogItem[]
}

type UsageLogsPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

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

export default function UsageLogsPage({ onNavigate, onLogout }: UsageLogsPageProps) {
  const [apiKey, setApiKey] = useState("")
  const [logs, setLogs] = useState<UsageLogItem[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState("")
  const [keyword, setKeyword] = useState("")
  const [resultFilter, setResultFilter] = useState<"all" | "success" | "fail">("all")

  useEffect(() => {
    const savedApiKey = localStorage.getItem("apiKey") ?? ""
    setApiKey(savedApiKey)
  }, [])

  useEffect(() => {
    if (!apiKey) {
      setLoading(false)
      setError("API 키가 없습니다. 먼저 로그인 후 apiKey를 저장해주세요.")
      return
    }

    const controller = new AbortController()

    async function fetchLogs() {
      try {
        setLoading(true)
        setError("")

        const response = await fetch(`${API_BASE_URL}/api/v1/dashboard/usage-logs`, {
          method: "GET",
          headers: {
            "X-API-Key": apiKey,
          },
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error("사용 로그 조회 실패")
        }

        const data: UsageLogsResponse = await response.json()
        setLogs(data.items ?? [])
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") return
        console.error(err)
        setError("사용 로그를 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchLogs()

    return () => controller.abort()
  }, [apiKey])

  async function reloadLogs() {
    if (!apiKey) return

    try {
      setRefreshing(true)

      const response = await fetch(`${API_BASE_URL}/api/v1/dashboard/usage-logs`, {
        method: "GET",
        headers: {
          "X-API-Key": apiKey,
        },
      })

      if (!response.ok) {
        throw new Error("사용 로그 새로고침 실패")
      }

      const data: UsageLogsResponse = await response.json()
      setLogs(data.items ?? [])
    } catch (err) {
      console.error(err)
      alert("사용 로그 새로고침에 실패했습니다.")
    } finally {
      setRefreshing(false)
    }
  }

  const filteredLogs = useMemo(() => {
    return logs.filter((item) => {
      const matchesKeyword =
        !keyword.trim() ||
        item.actionType.toLowerCase().includes(keyword.toLowerCase()) ||
        (item.targetId ?? "").toLowerCase().includes(keyword.toLowerCase()) ||
        (item.message ?? "").toLowerCase().includes(keyword.toLowerCase())

      const matchesResult =
        resultFilter === "all" ||
        (resultFilter === "success" && item.success) ||
        (resultFilter === "fail" && !item.success)

      return matchesKeyword && matchesResult
    })
  }, [logs, keyword, resultFilter])

  const successCount = logs.filter((item) => item.success).length
  const failCount = logs.filter((item) => !item.success).length

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[28px] border border-slate-200/70 bg-white p-6 shadow-sm">
            <p className="text-lg font-semibold">Usage Logs 불러오는 중...</p>
            <p className="mt-2 text-sm text-slate-500">최근 API 사용 기록을 확인하고 있어요.</p>
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[28px] border border-red-200 bg-white p-6 shadow-sm">
            <p className="text-lg font-semibold text-red-600">에러</p>
            <p className="mt-2 text-sm text-slate-600">{error}</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="usage" onLogout={onLogout} />

      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 rounded-[28px] border border-slate-200/70 bg-white/80 p-5 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
                  <span className="h-2 w-2 rounded-full bg-emerald-500" />
                  AutoHWPX SaaS
                </div>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Usage Logs</h1>
                <p className="mt-2 text-sm text-slate-500 sm:text-base">
                  API 호출 기록과 상태를 확인하는 공간입니다
                </p>
              </div>

              <button
                onClick={reloadLogs}
                disabled={refreshing}
                className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-sm hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <RefreshCw className={`h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
                새로고침
              </button>
            </div>
          </header>

          <section className="mb-6 grid gap-4 sm:grid-cols-3">
            <div className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
              <div className="p-5">
                <p className="text-sm text-slate-500">전체 로그 수</p>
                <p className="mt-3 text-3xl font-bold tracking-tight">{logs.length}</p>
              </div>
            </div>

            <div className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
              <div className="p-5">
                <p className="text-sm text-slate-500">성공</p>
                <p className="mt-3 text-3xl font-bold tracking-tight">{successCount}</p>
              </div>
            </div>

            <div className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
              <div className="p-5">
                <p className="text-sm text-slate-500">실패</p>
                <p className="mt-3 text-3xl font-bold tracking-tight">{failCount}</p>
              </div>
            </div>
          </section>

          <section className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
            <div className="flex flex-col gap-4 p-6 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="flex items-center gap-2 text-xl font-semibold">
                  <Activity className="h-5 w-5" />
                  사용 내역
                </h2>
                <p className="mt-2 text-sm text-slate-500">로그를 검색하고 결과 상태로 필터링할 수 있습니다.</p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
                  <Search className="h-4 w-4 text-slate-400" />
                  <input
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    placeholder="action, target, message 검색"
                    className="w-full bg-transparent outline-none placeholder:text-slate-400 sm:w-64"
                  />
                </div>

                <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm">
                  <Filter className="h-4 w-4 text-slate-400" />
                  <select
                    value={resultFilter}
                    onChange={(e) =>
                      setResultFilter(e.target.value as "all" | "success" | "fail")
                    }
                    className="bg-transparent outline-none"
                  >
                    <option value="all">전체</option>
                    <option value="success">성공</option>
                    <option value="fail">실패</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="p-6 pt-0">
              <div className="overflow-hidden rounded-2xl border border-slate-200">
                <div className="overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead className="bg-slate-50 text-slate-600">
                      <tr>
                        <th className="px-4 py-3 text-left font-semibold">작업유형</th>
                        <th className="px-4 py-3 text-left font-semibold">대상</th>
                        <th className="px-4 py-3 text-left font-semibold">메세지</th>
                        <th className="px-4 py-3 text-left font-semibold">결과</th>
                        <th className="px-4 py-3 text-left font-semibold">시간</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredLogs.length === 0 ? (
                        <tr className="border-t border-slate-200 bg-white">
                          <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                            표시할 사용 로그가 없습니다.
                          </td>
                        </tr>
                      ) : (
                        filteredLogs.map((item) => (
                          <tr key={item.id} className="border-t border-slate-200 bg-white">
                            <td className="px-4 py-3 font-medium">{item.actionType}</td>
                            <td className="px-4 py-3 text-slate-600">{item.targetId || "-"}</td>
                            <td className="max-w-[280px] px-4 py-3 text-slate-500">
                              <div className="truncate">{item.message || "-"}</div>
                            </td>
                            <td className="px-4 py-3">
                              <span
                                className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold ${
                                  item.success
                                    ? "bg-emerald-50 text-emerald-700"
                                    : "bg-red-50 text-red-700"
                                }`}
                              >
                                {item.success ? (
                                  <CheckCircle2 className="h-3.5 w-3.5" />
                                ) : (
                                  <XCircle className="h-3.5 w-3.5" />
                                )}
                                {item.success ? "Success" : "Fail"}
                              </span>
                            </td>
                            <td className="px-4 py-3 text-slate-500">
                              {formatDateTime(item.createdAt)}
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
    </>
  )
}