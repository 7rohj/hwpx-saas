import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import { useEffect, useState } from "react"
import {
  KeyRound,
  RefreshCw,
  Copy,
  CheckCircle2,
  Lock,
} from "lucide-react"

type ApiKeyResponse = {
  ok: boolean
  email: string
  apiKey: string | null
}

type ApiKeysPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "hhttps://api.autohwpx.com"

function maskApiKey(value: string) {
  if (value.length <= 12) return value
  return `${value.slice(0, 8)}••••••••${value.slice(-6)}`
}

export default function ApiKeysPage({ onNavigate, onLogout }: ApiKeysPageProps) {
  const [email, setEmail] = useState("")
  const [apiKey, setApiKey] = useState("")
  const [loading, setLoading] = useState(true)
  const [refreshingKey, setRefreshingKey] = useState(false)
  const [error, setError] = useState("")
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    const savedEmail = localStorage.getItem("userEmail") ?? ""
    const savedApiKey = localStorage.getItem("apiKey") ?? ""

    setEmail(savedEmail)
    setApiKey(savedApiKey)
  }, [])

  useEffect(() => {
    if (!email) {
      setLoading(false)
      setError("로그인 정보가 없습니다. 먼저 로그인해서 email / apiKey를 저장해주세요.")
      return
    }

    const controller = new AbortController()

    async function fetchApiKey() {
      try {
        setLoading(true)
        setError("")

        const response = await fetch(
          `${API_BASE_URL}/api/v1/dashboard/api-key?email=${encodeURIComponent(email)}`,
          {
            method: "GET",
            signal: controller.signal,
          }
        )

        if (!response.ok) {
          throw new Error("API 키 조회 실패")
        }

        const data: ApiKeyResponse = await response.json()

        setEmail(data.email ?? email)

        if (data.apiKey) {
          setApiKey(data.apiKey)
          localStorage.setItem("apiKey", data.apiKey)
        }
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") return
        console.error(err)
        setError("API 키 정보를 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchApiKey()

    return () => controller.abort()
  }, [email])

  async function handleRegenerateApiKey() {
    if (!email) {
      alert("email 정보가 없습니다.")
      return
    }

    try {
      setRefreshingKey(true)

      const response = await fetch(
        `${API_BASE_URL}/api/v1/dashboard/api-key/regenerate?email=${encodeURIComponent(email)}`,
        {
          method: "POST",
        }
      )

      if (!response.ok) {
        throw new Error("API 키 재발급 실패")
      }

      const data: ApiKeyResponse = await response.json()

      if (!data.apiKey) {
        throw new Error("새 API 키가 비어 있습니다.")
      }

      setApiKey(data.apiKey)
      localStorage.setItem("apiKey", data.apiKey)
      alert("API 키가 재발급되었습니다.")
    } catch (err) {
      console.error(err)
      alert("API 키 재발급에 실패했습니다.")
    } finally {
      setRefreshingKey(false)
    }
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(apiKey)
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    } catch (err) {
      console.error(err)
      alert("복사에 실패했습니다.")
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6">
            <p className="text-lg font-semibold text-[#1a2540]">API 키 페이지 불러오는 중...</p>
            <p className="mt-2 text-sm text-[#63708a]">계정 정보와 키 상태를 확인하고 있어요.</p>
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
            <p className="mt-3 text-xs text-slate-400">
              localStorage에 userEmail / apiKey가 저장되어 있는지도 확인해보세요.
            </p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="apikeys" onLogout={onLogout} />

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
                  My
                </h1>
                <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                  API 키 및 요금제를 확인할 수 있습니다
                </p>
              </div>

              <div className="rounded-2xl border border-[#dbe7ff] bg-white px-4 py-3 text-sm shadow-sm">
                <p className="font-medium text-[#1a2540]">{email}</p>
                <p className="text-[#63708a]">Starter Plan</p>
              </div>
            </div>
          </header>

          <div className="grid gap-6 lg:grid-cols-12">
            <aside className="space-y-6 lg:col-span-4">
              <div className="rounded-[32px] border border-[#dbe7ff] bg-white">
                <div className="p-6">
                  <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                    <Lock className="h-5 w-5 text-[#1f69ef]" />
                    키 상태
                  </h2>

                  <div className="mt-4 grid gap-4">
                    <div className="rounded-2xl border border-[#d7e5ff] bg-[#f7fbff] p-4">
                      <p className="text-sm text-[#63708a]">계정 이메일</p>
                      <p className="mt-2 font-semibold text-[#1a2540]">{email || "-"}</p>
                    </div>

                    <div className="rounded-2xl border border-[#d7e5ff] bg-[#f7fbff] p-4">
                      <p className="text-sm text-[#63708a]">키 상태</p>
                      <div className="mt-2">
                        <span className="inline-flex items-center gap-2 rounded-full bg-[#eef4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                          <CheckCircle2 className="h-4 w-4" />
                          Active
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </aside>

            <main className="space-y-6 lg:col-span-8">
              <section className="rounded-[32px] border border-[#dbe7ff] bg-white">
                <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                      <KeyRound className="h-5 w-5 text-[#1f69ef]" />
                      현재 API Key
                    </h2>
                    <p className="mt-2 text-sm text-[#63708a]">
                      현재 발급된 키입니다. 필요 시 복사 또는 재발급할 수 있습니다.
                    </p>
                  </div>

                  <div className="flex gap-2">
                    <button
                      onClick={handleCopy}
                      disabled={!apiKey}
                      className="inline-flex items-center gap-2 rounded-2xl bg-[#1f69ef] px-4 py-2 text-sm font-semibold text-white hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Copy className="h-4 w-4" />
                      {copied ? "복사됨" : "복사"}
                    </button>

                    <button
                      onClick={handleRegenerateApiKey}
                      disabled={refreshingKey}
                      className="inline-flex items-center gap-2 rounded-2xl bg-[#1f69ef] px-4 py-2 text-sm font-semibold text-white hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <RefreshCw className={`h-4 w-4 ${refreshingKey ? "animate-spin" : ""}`} />
                      재발급
                    </button>
                  </div>
                </div>

                <div className="px-6 pb-6">
                  <div className="overflow-hidden rounded-[24px] bg-[#111827] p-5 font-mono text-sm text-[#60a5fa] shadow-inner break-all">
                    {apiKey || "API Key 없음"}
                  </div>
                </div>
              </section>

              <section className="grid gap-4 sm:grid-cols-3">
                <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
                  <div className="h-1.5 bg-slate-200" />
                  <div className="p-5">
                    <p className="text-sm text-slate-500">미리보기</p>
                    <p className="mt-3 font-mono text-lg font-bold tracking-tight text-[#162033]">
                      {apiKey ? maskApiKey(apiKey) : "-"}
                    </p>
                  </div>
                </div>

                <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
                  <div className="h-1.5 bg-slate-200" />
                  <div className="p-5">
                    <p className="text-sm text-slate-500">저장위치</p>
                    <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                      localStorage
                    </p>
                  </div>
                </div>

                <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
                  <div className="h-1.5 bg-slate-200" />
                  <div className="p-5">
                    <p className="text-sm text-slate-500">사용환경</p>
                    <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                      Starter
                    </p>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
      </div>
    </>
  )
}