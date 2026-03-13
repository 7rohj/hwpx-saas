import { useEffect, useMemo, useRef, useState } from "react"
import {
  Search,
  FileText,
  KeyRound,
  Activity,
  LayoutDashboard,
  CreditCard,
  FolderOpen,
  RefreshCw,
  Upload,
  Download,
  X,
  Braces,
} from "lucide-react"

type TemplateItem = {
  templateId: string
  originalName: string
  size: number
  uploadedAt: string
}

type GeneratedFileItem = {
  id: number
  templateId: string
  fileId: string
  originalFilename: string
  r2ObjectKey: string
  size: number
  status: string
  createdAt: string
  downloadUrl: string
}

type UsageLogItem = {
  id: number
  actionType: string
  targetId: string | null
  success: boolean
  message: string | null
  createdAt: string
}

type ApiKeyResponse = {
  ok: boolean
  email: string
  apiKey: string | null
}

type TemplateListResponse = {
  ok: boolean
  count: number
  items: TemplateItem[]
}

type GeneratedFilesResponse = {
  ok: boolean
  count: number
  items: GeneratedFileItem[]
}

type UsageLogsResponse = {
  ok: boolean
  count: number
  items: UsageLogItem[]
}

type TemplateVarsResponse = {
  ok: boolean
  templateId: string
  originalName: string
  varsCount: number
  vars: string[]
}

type GenerateResponse = {
  ok?: boolean
  message?: string
  fileId?: string
  downloadUrl?: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

function formatDate(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date)
}

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

function formatBytes(bytes?: number | null) {
  if (!bytes || bytes < 0) return "-"
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function Dashboard() {
  const [email, setEmail] = useState("")
  const [apiKey, setApiKey] = useState("")
  const [templates, setTemplates] = useState<TemplateItem[]>([])
  const [files, setFiles] = useState<GeneratedFileItem[]>([])
  const [logs, setLogs] = useState<UsageLogItem[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshingKey, setRefreshingKey] = useState(false)
  const [error, setError] = useState("")

  const [varsOpen, setVarsOpen] = useState(false)
  const [varsLoading, setVarsLoading] = useState(false)
  const [varsError, setVarsError] = useState("")
  const [varsData, setVarsData] = useState<TemplateVarsResponse | null>(null)

  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const [selectedTemplateId, setSelectedTemplateId] = useState("")
  const [generateVars, setGenerateVars] = useState<string[]>([])
  const [generateValues, setGenerateValues] = useState<Record<string, string>>({})
  const [generateVarsLoading, setGenerateVarsLoading] = useState(false)
  const [generating, setGenerating] = useState(false)

  useEffect(() => {
    const savedEmail = localStorage.getItem("userEmail") ?? ""
    const savedApiKey = localStorage.getItem("apiKey") ?? ""

    setEmail(savedEmail)
    setApiKey(savedApiKey)
  }, [])

  useEffect(() => {
    if (!email || !apiKey) {
      setLoading(false)
      setError("로그인 정보가 없습니다. 먼저 로그인해서 email / apiKey를 저장해주세요.")
      return
    }

    const controller = new AbortController()

    async function fetchDashboardData() {
      try {
        setLoading(true)
        setError("")

        const [apiKeyRes, templateRes, fileRes, logRes] = await Promise.all([
          fetch(
            `${API_BASE_URL}/api/v1/dashboard/api-key?email=${encodeURIComponent(email)}`,
            {
              method: "GET",
              signal: controller.signal,
            }
          ),
          fetch(`${API_BASE_URL}/api/v1/template/list`, {
            method: "GET",
            headers: {
              "X-API-Key": apiKey,
            },
            signal: controller.signal,
          }),
          fetch(`${API_BASE_URL}/api/v1/dashboard/generated-files`, {
            method: "GET",
            headers: {
              "X-API-Key": apiKey,
            },
            signal: controller.signal,
          }),
          fetch(`${API_BASE_URL}/api/v1/dashboard/usage-logs`, {
            method: "GET",
            headers: {
              "X-API-Key": apiKey,
            },
            signal: controller.signal,
          }),
        ])

        if (!apiKeyRes.ok) throw new Error("API 키 조회 실패")
        if (!templateRes.ok) throw new Error("템플릿 목록 조회 실패")
        if (!fileRes.ok) throw new Error("생성 파일 목록 조회 실패")
        if (!logRes.ok) throw new Error("사용 로그 조회 실패")

        const apiKeyData: ApiKeyResponse = await apiKeyRes.json()
        const templateData: TemplateListResponse = await templateRes.json()
        const fileData: GeneratedFilesResponse = await fileRes.json()
        const logData: UsageLogsResponse = await logRes.json()

        setEmail(apiKeyData.email ?? email)

        if (apiKeyData.apiKey) {
          setApiKey(apiKeyData.apiKey)
          localStorage.setItem("apiKey", apiKeyData.apiKey)
        }

        setTemplates(templateData.items ?? [])
        setFiles(fileData.items ?? [])
        setLogs(logData.items ?? [])

        if (!selectedTemplateId && templateData.items?.length > 0) {
          setSelectedTemplateId(templateData.items[0].templateId)
        }
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") return
        console.error(err)
        setError("대시보드 데이터를 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    fetchDashboardData()

    return () => controller.abort()
  }, [email, apiKey])

  useEffect(() => {
    if (!selectedTemplateId || !apiKey) {
      setGenerateVars([])
      setGenerateValues({})
      return
    }

    const controller = new AbortController()

    async function loadGenerateVars() {
      try {
        setGenerateVarsLoading(true)

        const response = await fetch(
          `${API_BASE_URL}/api/v1/template/${selectedTemplateId}/vars`,
          {
            method: "GET",
            headers: {
              "X-API-Key": apiKey,
            },
            signal: controller.signal,
          }
        )

        const data: TemplateVarsResponse = await response.json()

        if (!response.ok || !data.ok) {
          throw new Error("생성용 vars 조회 실패")
        }

        setGenerateVars(data.vars ?? [])
        setGenerateValues((prev) => {
          const next: Record<string, string> = {}
          for (const key of data.vars ?? []) {
            next[key] = prev[key] ?? ""
          }
          return next
        })
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") return
        console.error(err)
        setGenerateVars([])
        setGenerateValues({})
      } finally {
        setGenerateVarsLoading(false)
      }
    }

    loadGenerateVars()

    return () => controller.abort()
  }, [selectedTemplateId, apiKey])

  const thisMonthUsage = useMemo(() => {
    const now = new Date()
    return logs.filter((log) => {
      const d = new Date(log.createdAt)
      return (
        !Number.isNaN(d.getTime()) &&
        d.getFullYear() === now.getFullYear() &&
        d.getMonth() === now.getMonth()
      )
    }).length
  }, [logs])

  async function reloadGeneratedFiles() {
    if (!apiKey) return

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/dashboard/generated-files`, {
        method: "GET",
        headers: {
          "X-API-Key": apiKey,
        },
      })

      if (!response.ok) {
        throw new Error("생성 파일 목록 새로고침 실패")
      }

      const data: GeneratedFilesResponse = await response.json()
      setFiles(data.items ?? [])
    } catch (err) {
      console.error(err)
    }
  }

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

  async function handleUploadTemplate(file: File) {
    if (!apiKey) {
      alert("API Key가 없습니다.")
      return
    }

    try {
      setUploading(true)

      const formData = new FormData()
      formData.append("template", file)
      formData.append("open", "{{")
      formData.append("close", "}}")

      const response = await fetch(`${API_BASE_URL}/api/v1/template/upload`, {
        method: "POST",
        headers: {
          "X-API-Key": apiKey,
        },
        body: formData,
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        throw new Error(data.message ?? "템플릿 업로드 실패")
      }

      const newItem: TemplateItem = {
        templateId: data.templateId,
        originalName: data.originalName,
        size: data.size,
        uploadedAt: data.uploadedAt,
      }

      setTemplates((prev) => [newItem, ...prev])
      setSelectedTemplateId(data.templateId)
      alert("템플릿 업로드 완료")
    } catch (err) {
      console.error(err)
      alert("템플릿 업로드에 실패했습니다.")
    } finally {
      setUploading(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ""
      }
    }
  }

  async function handleGenerateDocument() {
    if (!apiKey) {
      alert("API Key가 없습니다.")
      return
    }

    if (!selectedTemplateId) {
      alert("먼저 템플릿을 선택해주세요.")
      return
    }

    if (generateVars.length === 0) {
      alert("생성 가능한 변수가 없습니다.")
      return
    }

    const hasEmptyRequiredValue = generateVars.some(
      (key) => !String(generateValues[key] ?? "").trim()
    )

    if (hasEmptyRequiredValue) {
      alert("모든 변수 값을 입력해주세요.")
      return
    }

    try {
      setGenerating(true)

      const elements = generateVars.map((key) => ({
        placeholder: key,
        type: "text",
        content: generateValues[key] ?? "",
      }))

      const response = await fetch(`${API_BASE_URL}/api/v1/generate`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-API-Key": apiKey,
        },
        body: JSON.stringify({
          templateId: selectedTemplateId,
          outputFilename: "output.hwpx",
          elements,
        }),
      })

      const data: GenerateResponse = await response.json()
      console.log("generate response:", data)

      if (!response.ok || data.ok === false) {
        throw new Error(data.message ?? "문서 생성 실패")
      }

      alert("문서 생성 완료")
      await reloadGeneratedFiles()
    } catch (err) {
      console.error(err)
      alert("문서 생성에 실패했습니다.")
    } finally {
      setGenerating(false)
    }
  }

  async function handleOpenVars(templateId: string) {
    if (!apiKey) {
      alert("API Key가 없습니다.")
      return
    }

    try {
      setVarsOpen(true)
      setVarsLoading(true)
      setVarsError("")
      setVarsData(null)

      const response = await fetch(`${API_BASE_URL}/api/v1/template/${templateId}/vars`, {
        method: "GET",
        headers: {
          "X-API-Key": apiKey,
        },
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        throw new Error(data.message ?? "Vars 조회 실패")
      }

      setVarsData(data)
    } catch (err) {
      console.error(err)
      setVarsError("템플릿 변수 조회에 실패했습니다.")
    } finally {
      setVarsLoading(false)
    }
  }

  async function handleDownloadFile(file: GeneratedFileItem) {
    try {
      const response = await fetch(`${API_BASE_URL}${file.downloadUrl}`, {
        method: "GET",
        headers: {
          "X-API-Key": apiKey,
        },
      })

      if (!response.ok) {
        throw new Error("다운로드 실패")
      }

      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = file.originalFilename || "download.hwpx"
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      console.error(err)
      alert("파일 다운로드에 실패했습니다.")
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[28px] border border-slate-200/70 bg-white p-6 shadow-sm">
            <p className="text-lg font-semibold">대시보드 불러오는 중...</p>
            <p className="mt-2 text-sm text-slate-500">API 연결 상태를 확인하고 있어요.</p>
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
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 rounded-[28px] border border-slate-200/70 bg-white/80 p-5 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
                  <span className="h-2 w-2 rounded-full bg-emerald-500" />
                  AutoHWPX SaaS
                </div>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Dashboard</h1>
                <p className="mt-2 text-sm text-slate-500 sm:text-base">
                  내 API 키, 템플릿, 생성 문서를 한눈에 관리하는 공간
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
                  <Search className="h-4 w-4 text-slate-400" />
                  <span className="text-slate-400">검색은 다음 단계에서 연결</span>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-sm">
                  <p className="font-medium">{email}</p>
                  <p className="text-slate-500">Starter Plan</p>
                </div>
              </div>
            </div>
          </header>

          <div className="grid gap-6 lg:grid-cols-12">
            <aside className="lg:col-span-3">
              <div className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
                <div className="p-4">
                  <div className="mb-4 px-2 pt-1 text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
                    Menu
                  </div>
                  <nav className="space-y-2">
                    <button className="flex w-full items-center gap-3 rounded-2xl bg-slate-900 px-4 py-3 text-left text-sm font-medium text-white shadow-sm">
                      <LayoutDashboard className="h-4 w-4" />
                      Dashboard
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-slate-50">
                      <FolderOpen className="h-4 w-4" />
                      Templates
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-slate-50">
                      <FileText className="h-4 w-4" />
                      Generated Files
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-slate-50">
                      <Activity className="h-4 w-4" />
                      Usage Logs
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-slate-50">
                      <CreditCard className="h-4 w-4" />
                      Billing
                    </button>
                  </nav>
                </div>
              </div>
            </aside>

            <main className="space-y-6 lg:col-span-9">
              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  ["Templates", String(templates.length)],
                  ["Generated Files", String(files.length)],
                  ["This Month Usage", String(thisMonthUsage)],
                  ["Plan", "Starter"],
                ].map(([label, value]) => (
                  <div key={label} className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
                    <div className="p-5">
                      <p className="text-sm text-slate-500">{label}</p>
                      <p className="mt-3 text-3xl font-bold tracking-tight">{value}</p>
                    </div>
                  </div>
                ))}
              </section>

              <section className="grid gap-6 xl:grid-cols-3">
                <div className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm xl:col-span-2">
                  <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                    <div>
                      <h2 className="flex items-center gap-2 text-xl font-semibold">
                        <KeyRound className="h-5 w-5" />
                        API Key
                      </h2>
                      <p className="mt-2 text-sm text-slate-500">
                        문서 생성, 템플릿 업로드, 대시보드 조회에 사용하는 개인 키
                      </p>
                    </div>
                    <button
                      onClick={handleRegenerateApiKey}
                      disabled={refreshingKey}
                      className="rounded-2xl border px-4 py-2 text-sm hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <span className="inline-flex items-center gap-2">
                        <RefreshCw className={`h-4 w-4 ${refreshingKey ? "animate-spin" : ""}`} />
                        재발급
                      </span>
                    </button>
                  </div>
                  <div className="p-6">
                    <div className="overflow-hidden rounded-2xl bg-slate-950 p-4 font-mono text-sm text-emerald-300 shadow-inner break-all">
                      {apiKey || "API Key 없음"}
                    </div>
                  </div>
                </div>

                <div className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
                  <div className="p-6 pb-0">
                    <h2 className="text-xl font-semibold">Quick Actions</h2>
                    <p className="text-sm text-slate-500">자주 쓰는 기능을 빠르게 실행</p>
                  </div>

                  <div className="space-y-3 p-6">
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept=".hwpx"
                      className="hidden"
                      onChange={(e) => {
                        const file = e.target.files?.[0]
                        if (file) {
                          handleUploadTemplate(file)
                        }
                      }}
                    />

                    <button
                      onClick={() => fileInputRef.current?.click()}
                      disabled={uploading}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-3 text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <Upload className="h-4 w-4" />
                      {uploading ? "업로드 중..." : "템플릿 업로드"}
                    </button>

                    <select
                      value={selectedTemplateId}
                      onChange={(e) => setSelectedTemplateId(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
                    >
                      <option value="">템플릿 선택</option>
                      {templates.map((item) => (
                        <option key={item.templateId} value={item.templateId}>
                          {item.originalName}
                        </option>
                      ))}
                    </select>

                    {generateVarsLoading ? (
                      <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
                        템플릿 변수 불러오는 중...
                      </div>
                    ) : generateVars.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-3 text-sm text-slate-500">
                        선택한 템플릿에 입력할 변수가 없습니다.
                      </div>
                    ) : (
                      generateVars.map((key) => (
                        <input
                          key={key}
                          type="text"
                          value={generateValues[key] ?? ""}
                          onChange={(e) =>
                            setGenerateValues((prev) => ({
                              ...prev,
                              [key]: e.target.value,
                            }))
                          }
                          placeholder={`${key} 값 입력`}
                          className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
                        />
                      ))
                    )}

                    <button
                      onClick={handleGenerateDocument}
                      disabled={generating || generateVarsLoading || generateVars.length === 0}
                      className="w-full rounded-2xl border px-4 py-3 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {generating ? "문서 생성 중..." : "문서 생성 테스트"}
                    </button>

                    <button
                      onClick={reloadGeneratedFiles}
                      className="w-full rounded-2xl border px-4 py-3 hover:bg-slate-50"
                    >
                      생성 파일 새로고침
                    </button>
                  </div>
                </div>
              </section>

              <section className="grid gap-6 xl:grid-cols-2">
                <div className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
                  <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                    <div>
                      <h2 className="text-xl font-semibold">Templates</h2>
                      <p className="text-sm text-slate-500">내가 업로드한 템플릿 목록</p>
                    </div>
                    <button className="rounded-2xl border px-4 py-2 text-sm hover:bg-slate-50">
                      전체 보기
                    </button>
                  </div>
                  <div className="space-y-3 p-6">
                    {templates.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-slate-200 p-6 text-sm text-slate-500">
                        업로드된 템플릿이 없습니다.
                      </div>
                    ) : (
                      templates.slice(0, 6).map((item) => (
                        <div
                          key={item.templateId}
                          className="rounded-2xl border border-slate-200 p-4 transition hover:border-slate-300 hover:shadow-sm"
                        >
                          <div className="flex items-center justify-between gap-4">
                            <div className="min-w-0">
                              <p className="truncate font-semibold">{item.originalName}</p>
                              <p className="mt-1 text-sm text-slate-500">
                                업로드일 {formatDate(item.uploadedAt)} · {formatBytes(item.size)}
                              </p>
                            </div>
                            <button
                              onClick={() => handleOpenVars(item.templateId)}
                              className="inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-sm hover:bg-slate-50"
                            >
                              <Braces className="h-4 w-4" />
                              Vars
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
                  <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                    <div>
                      <h2 className="text-xl font-semibold">Generated Files</h2>
                      <p className="text-sm text-slate-500">최근 생성된 문서</p>
                    </div>
                    <button
                      onClick={reloadGeneratedFiles}
                      className="rounded-2xl border px-4 py-2 text-sm hover:bg-slate-50"
                    >
                      전체 보기
                    </button>
                  </div>
                  <div className="space-y-3 p-6">
                    {files.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-slate-200 p-6 text-sm text-slate-500">
                        생성된 문서가 없습니다.
                      </div>
                    ) : (
                      files.slice(0, 6).map((item) => (
                        <div
                          key={item.fileId}
                          className="rounded-2xl border border-slate-200 p-4 transition hover:border-slate-300 hover:shadow-sm"
                        >
                          <div className="flex items-center justify-between gap-4">
                            <div className="min-w-0">
                              <p className="truncate font-semibold">{item.originalFilename}</p>
                              <p className="mt-1 text-sm text-slate-500">
                                생성일 {formatDate(item.createdAt)} · {formatBytes(item.size)}
                              </p>
                            </div>

                            <div className="flex items-center gap-2">
                              <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                                {item.status}
                              </span>
                              <button
                                onClick={() => handleDownloadFile(item)}
                                className="inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-sm hover:bg-slate-50"
                              >
                                <Download className="h-4 w-4" />
                                다운로드
                              </button>
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </section>

              <div className="rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
                <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                  <div>
                    <h2 className="text-xl font-semibold">Usage Logs</h2>
                    <p className="text-sm text-slate-500">최근 API 사용 기록</p>
                  </div>
                  <button className="rounded-2xl border px-4 py-2 text-sm hover:bg-slate-50">
                    전체 보기
                  </button>
                </div>
                <div className="p-6">
                  <div className="overflow-hidden rounded-2xl border border-slate-200">
                    <table className="min-w-full text-sm">
                      <thead className="bg-slate-50 text-slate-600">
                        <tr>
                          <th className="px-4 py-3 text-left font-semibold">Action</th>
                          <th className="px-4 py-3 text-left font-semibold">Target</th>
                          <th className="px-4 py-3 text-left font-semibold">Result</th>
                          <th className="px-4 py-3 text-left font-semibold">Time</th>
                        </tr>
                      </thead>
                      <tbody>
                        {logs.length === 0 ? (
                          <tr className="border-t border-slate-200 bg-white">
                            <td colSpan={4} className="px-4 py-6 text-center text-slate-500">
                              사용 로그가 없습니다.
                            </td>
                          </tr>
                        ) : (
                          logs.slice(0, 10).map((item) => (
                            <tr key={item.id} className="border-t border-slate-200 bg-white">
                              <td className="px-4 py-3 font-medium">{item.actionType}</td>
                              <td className="px-4 py-3 text-slate-600">{item.targetId || "-"}</td>
                              <td className="px-4 py-3">
                                <span
                                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                    item.success
                                      ? "bg-emerald-50 text-emerald-700"
                                      : "bg-red-50 text-red-700"
                                  }`}
                                >
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
            </main>
          </div>
        </div>
      </div>

      {varsOpen ? (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 px-4">
          <div className="w-full max-w-lg rounded-[28px] border border-slate-200 bg-white p-6 shadow-xl">
            <div className="mb-4 flex items-start justify-between gap-4">
              <div>
                <h3 className="text-xl font-semibold">Template Vars</h3>
                <p className="mt-1 text-sm text-slate-500">
                  템플릿에서 추출된 placeholder 목록
                </p>
              </div>
              <button
                onClick={() => setVarsOpen(false)}
                className="rounded-xl border p-2 hover:bg-slate-50"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {varsLoading ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500">
                Vars 불러오는 중...
              </div>
            ) : varsError ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-sm text-red-600">
                {varsError}
              </div>
            ) : varsData ? (
              <div className="space-y-4">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <p className="font-medium">{varsData.originalName}</p>
                  <p className="mt-1 text-sm text-slate-500">
                    templateId: {varsData.templateId}
                  </p>
                </div>

                <div className="rounded-2xl border border-slate-200">
                  <div className="border-b border-slate-200 px-4 py-3 text-sm font-medium text-slate-700">
                    변수 개수: {varsData.varsCount}
                  </div>
                  <div className="max-h-72 space-y-2 overflow-y-auto p-4">
                    {varsData.vars.length === 0 ? (
                      <p className="text-sm text-slate-500">추출된 변수가 없습니다.</p>
                    ) : (
                      varsData.vars.map((item) => (
                        <div
                          key={item}
                          className="rounded-xl border border-slate-200 bg-white px-3 py-2 font-mono text-sm text-slate-700"
                        >
                          {item}
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </>
  )
}