import type { PageType } from "../App"
import TopNav from "../components/TopNav"
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

type DashboardProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "https://api.autohwpx.com"

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

function isTableVarName(name: string) {
  const lower = name.toLowerCase()
  return lower.includes("table") || lower.includes("rows") || lower.includes("items")
}

export default function Dashboard({ onNavigate, onLogout }: DashboardProps) {
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
  const [textVarNames, setTextVarNames] = useState<string[]>([])
  const [tableVarNames, setTableVarNames] = useState<string[]>([])
  const [textValues, setTextValues] = useState<Record<string, string>>({})
  const [tableValues, setTableValues] = useState<Record<string, string>>({})
  const [generateVarsLoading, setGenerateVarsLoading] = useState(false)
  const [generating, setGenerating] = useState(false)

  const [inputMode, setInputMode] = useState<"vars" | "json">("vars")
  const [jsonPayload, setJsonPayload] = useState(`{
  "templateId": "",
  "outputFilename": "output.hwpx",
  "elements": [
    {
      "placeholder": "title",
      "type": "text",
      "content": "문서 제목입니다"
    }
  ],
  "global_variables": {}
}`)

  const [highlightedFileId, setHighlightedFileId] = useState<string | null>(null)

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
            credentials: "include",
            signal: controller.signal,
          }),
          fetch(`${API_BASE_URL}/api/v1/dashboard/generated-files`, {
            method: "GET",
            headers: {
              "X-API-Key": apiKey,
            },
            credentials: "include",
            signal: controller.signal,
          }),
          fetch(`${API_BASE_URL}/api/v1/dashboard/usage-logs`, {
            method: "GET",
            headers: {
              "X-API-Key": apiKey,
            },
            credentials: "include",
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
      setTextVarNames([])
      setTableVarNames([])
      setTextValues({})
      setTableValues({})
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

        const allVars = data.vars ?? []
        const detectedTableVars = allVars.filter((key) => isTableVarName(key))
        const detectedTextVars = allVars.filter((key) => !isTableVarName(key))

        setTextVarNames(detectedTextVars)
        setTableVarNames(detectedTableVars)

        setTextValues((prev) => {
          const next: Record<string, string> = {}
          for (const key of detectedTextVars) {
            next[key] = prev[key] ?? ""
          }
          return next
        })

        setTableValues((prev) => {
          const next: Record<string, string> = {}
          for (const key of detectedTableVars) {
            next[key] = prev[key] ?? '[["항목","값"],["예시1","100"],["예시2","200"]]'
          }
          return next
        })

        const sampleElements = [
          ...detectedTextVars.map((key) => ({
            placeholder: key,
            type: "text",
            content: "",
          })),
          ...detectedTableVars.map((key) => ({
            placeholder: key,
            type: "table",
            data: [
              ["구분", "값1", "값2"],
              ["예시1", "A", "B"],
              ["예시2", "C", "D"],
            ],
          })),
        ]

        setJsonPayload(
          JSON.stringify(
            {
              templateId: selectedTemplateId,
              outputFilename: "output.hwpx",
              elements: sampleElements,
              global_variables: {},
            },
            null,
            2
          )
        )
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") return
        console.error(err)
        setTextVarNames([])
        setTableVarNames([])
        setTextValues({})
        setTableValues({})
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

    if (inputMode === "json") {
      try {
        setGenerating(true)

        let parsedBody: any
        try {
          parsedBody = JSON.parse(jsonPayload)
        } catch {
          throw new Error("JSON 형식이 올바르지 않습니다.")
        }

        if (!parsedBody.templateId) {
          parsedBody.templateId = selectedTemplateId
        }

        if (!parsedBody.templateId) {
          throw new Error("templateId가 없습니다.")
        }

        const response = await fetch(`${API_BASE_URL}/api/v1/generate`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-API-Key": apiKey,
          },
          body: JSON.stringify(parsedBody),
        })

        const data: GenerateResponse = await response.json()
        console.log("generate response:", data)

        if (!response.ok || data.ok === false) {
          throw new Error(data.message ?? "문서 생성 실패")
        }

        await reloadGeneratedFiles()

        if (data.fileId) {
          setHighlightedFileId(data.fileId)
          setTimeout(() => setHighlightedFileId(null), 3500)
        }

        alert("문서 생성 완료")
      } catch (err) {
        console.error(err)
        if (err instanceof Error) {
          alert(err.message)
        } else {
          alert("문서 생성에 실패했습니다.")
        }
      } finally {
        setGenerating(false)
      }

      return
    }

    if (!selectedTemplateId) {
      alert("먼저 템플릿을 선택해주세요.")
      return
    }

    if (textVarNames.length === 0 && tableVarNames.length === 0) {
      alert("생성 가능한 변수가 없습니다.")
      return
    }

    const hasEmptyTextValue = textVarNames.some((key) => !String(textValues[key] ?? "").trim())

    if (hasEmptyTextValue) {
      alert("텍스트 변수 값을 모두 입력해주세요.")
      return
    }

    const hasEmptyTableValue = tableVarNames.some((key) => !String(tableValues[key] ?? "").trim())

    if (hasEmptyTableValue) {
      alert("표 변수 JSON 값을 입력해주세요.")
      return
    }

    try {
      setGenerating(true)

      const textElements = textVarNames.map((key) => ({
        placeholder: key,
        type: "text",
        content: textValues[key] ?? "",
      }))

      const tableElements = tableVarNames.map((key) => {
        let parsedData: string[][] = []

        try {
          parsedData = JSON.parse(tableValues[key] ?? "[]")
        } catch {
          throw new Error(`${key} 표 JSON 형식이 올바르지 않습니다.`)
        }

        if (!Array.isArray(parsedData)) {
          throw new Error(`${key} 표 데이터는 2차원 배열이어야 합니다.`)
        }

        return {
          placeholder: key,
          type: "table",
          data: parsedData,
        }
      })

      const elements = [...textElements, ...tableElements]

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

      await reloadGeneratedFiles()

      if (data.fileId) {
        setHighlightedFileId(data.fileId)
        setTimeout(() => setHighlightedFileId(null), 3500)
      }

      alert("문서 생성 완료")
    } catch (err) {
      console.error(err)
      if (err instanceof Error) {
        alert(err.message)
      } else {
        alert("문서 생성에 실패했습니다.")
      }
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
      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6 ">
            <p className="text-lg font-semibold text-[#1a2540]">대시보드 불러오는 중...</p>
            <p className="mt-2 text-sm text-[#63708a]">API 연결 상태를 확인하고 있어요.</p>
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="rounded-[32px] border border-red-200 bg-white p-6 shadow-[0_16px_40px_rgba(31,105,239,0.08)]">
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
      <TopNav onNavigate={onNavigate} currentPage="dashboard" onLogout={onLogout} />

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
                  대시보드
                </h1>
                <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                  내 API 키, 템플릿, 생성 문서를 한눈에 관리하는 공간
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                <div className="flex items-center gap-2 rounded-2xl border border-[#dbe7ff] bg-[#f3f8ff] px-4 py-3 text-sm">
                  <Search className="h-4 w-4 text-[#7c8aa5]" />
                  <span className="text-[#63708a]">검색은 다음 단계에서 연결</span>
                </div>
                <div className="rounded-2xl border border-[#dbe7ff] bg-white px-4 py-3 text-sm shadow-sm">
                  <p className="font-medium text-[#1a2540]">{email}</p>
                  <p className="text-[#63708a]">Starter Plan</p>
                </div>
              </div>
            </div>
          </header>

          <div className="grid gap-6 lg:grid-cols-12">
            <aside className="space-y-6 lg:col-span-3">
              <div className="rounded-[32px] border border-[#d7e5ff] bg-white/95">
                <div className="p-4">
                  <div className="mb-4 px-2 pt-1 text-xs font-semibold uppercase tracking-[0.2em] text-[#8a97b3]">
                    메뉴
                  </div>
                  <nav className="space-y-2">
                    <button className="flex w-full items-center gap-3 rounded-2xl bg-[#1f69ef] px-4 py-3 text-left text-sm font-semibold text-white ">
                      <LayoutDashboard className="h-4 w-4" />
                      대시보드
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-[#f3f8ff] hover:text-[#1f69ef]">
                      <FolderOpen className="h-4 w-4" />
                      템플릿
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-[#f3f8ff] hover:text-[#1f69ef]">
                      <FileText className="h-4 w-4" />
                      생성문서
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-[#f3f8ff] hover:text-[#1f69ef]">
                      <Activity className="h-4 w-4" />
                      사용량 로그
                    </button>
                    <button className="flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-medium text-slate-600 transition hover:bg-[#f3f8ff] hover:text-[#1f69ef]">
                      <CreditCard className="h-4 w-4" />
                      요금운영
                    </button>
                  </nav>
                </div>
              </div>

              <div className="rounded-[32px] border border-[#d7e5ff] bg-white/95 ">
                <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                  <div>
                    <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                      <KeyRound className="h-5 w-5 text-[#1f69ef]" />
                      API Key
                    </h2>
                    <p className="mt-2 text-sm text-[#63708a]">
                      문서 생성, 템플릿 업로드, 대시보드 조회에 사용하는 개인 키
                    </p>
                  </div>
                  <button
                    onClick={handleRegenerateApiKey}
                    disabled={refreshingKey}
                    className="rounded-2xl bg-[#1f69ef] px-4 py-2 text-sm font-semibold text-white hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <span className="items-center gap-2">
                      <RefreshCw className={`h-4 w-4 ${refreshingKey ? "animate-spin" : ""}`} />
                      
                    </span>
                  </button>
                </div>
                <div className="p-6">
                  <div className="overflow-hidden rounded-2xl bg-[#111827] p-4 font-mono text-sm text-[#60a5fa] shadow-inner break-all">
                    {apiKey || "API Key 없음"}
                  </div>
                </div>
              </div>
            </aside>

            <main className="space-y-6 lg:col-span-9">
              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  ["템플릿", String(templates.length)],
                  ["생성문서", String(files.length)],
                  ["이번 달 사용량", String(thisMonthUsage)],
                  ["요금제", "스타터"],
                ].map(([label, value]) => (
                  <div
                    key={label}
                    className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white shadow-sm"
                  >
                    <div className="h-1.5 bg-slate-200" />

                    <div className="p-5">
                      <div className="flex items-center justify-between gap-3">
                        <p className="text-sm text-slate-500">{label}</p>

                        {label === "요금제" ? (
                          <button
                            type="button"
                            onClick={() => onNavigate("pricing")}
                            className="text-xs font-medium text-slate-400 hover:text-[#1f69ef]"
                          >
                            업그레이드
                          </button>
                        ) : null}
                      </div>

                      <p className="mt-3 text-3xl font-bold tracking-tight">{value}</p>
                    </div>
                  </div>
                ))}
              </section>

              <section>
                <div className="rounded-[32px] border border-[#dbe7ff] bg-white ">
                  <div className="p-6 pb-0 text-center">
                    <h2 className="text-xl font-semibold text-[#162033]">빠른 실행</h2>
                    <p className="text-sm text-[#63708a]">자주 쓰는 기능을 빠르게 실행하세요</p>
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
                      className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#1f69ef] px-4 py-3 font-semibold text-white shadow-[0_10px_24px_rgba(31,105,239,0.22)] hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <Upload className="h-4 w-4" />
                      {uploading ? "업로드 중..." : "템플릿 업로드"}
                    </button>

                    <select
                      value={selectedTemplateId}
                      onChange={(e) => setSelectedTemplateId(e.target.value)}
                      className="w-full rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3 text-sm text-[#1a2540] outline-none"
                    >
                      <option value="">템플릿 선택</option>
                      {templates.map((item) => (
                        <option key={item.templateId} value={item.templateId}>
                          {item.originalName}
                        </option>
                      ))}
                    </select>

                    <div className="grid grid-cols-2 gap-2">
                      <button
                        type="button"
                        onClick={() => setInputMode("vars")}
                        className={`rounded-2xl px-4 py-2 text-sm font-medium transition ${
                          inputMode === "vars"
                            ? "bg-[#1f69ef] text-white "
                            : "border border-[#d7e5ff] bg-white text-slate-700 hover:bg-[#f5f9ff]"//shadow-[0_8px_18px_rgba(31,105,239,0.20)]
                        }`}
                      >
                        방법 1 · Vars 입력
                      </button>

                      <button
                        type="button"
                        onClick={() => setInputMode("json")}
                        className={`rounded-2xl px-4 py-2 text-sm font-medium transition ${
                          inputMode === "json"
                            ? "bg-[#1f69ef] text-white shadow-[0_8px_18px_rgba(31,105,239,0.20)]"
                            : "border border-[#d7e5ff] bg-white text-slate-700 hover:bg-[#f5f9ff]"
                        }`}
                      >
                        방법 2 · JSON 입력
                      </button>
                    </div>

                    {inputMode === "json" ? (
                      <div className="space-y-2 text-left">
                        <p className="text-sm font-medium text-[#1a2540]">JSON Payload</p>
                        <textarea
                          value={jsonPayload}
                          onChange={(e) => setJsonPayload(e.target.value)}
                          rows={20}
                          className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm font-mono outline-none"
                          placeholder="문서 생성용 JSON을 입력하세요."
                        />
                      </div>
                    ) : generateVarsLoading ? (
                      <div className="rounded-2xl border border-[#d7e5ff] bg-[#f3f8ff] px-4 py-3 text-sm text-[#63708a]">
                        템플릿 변수 불러오는 중...
                      </div>
                    ) : (
                      <>
                        {textVarNames.length === 0 && tableVarNames.length === 0 ? (
                          <div className="rounded-2xl border border-dashed border-[#d7e5ff] px-4 py-3 text-sm text-[#63708a]">
                            선택한 템플릿에 입력할 변수가 없습니다.
                          </div>
                        ) : null}

                        {textVarNames.map((key) => (
                          <input
                            key={key}
                            type="text"
                            value={textValues[key] ?? ""}
                            onChange={(e) =>
                              setTextValues((prev) => ({
                                ...prev,
                                [key]: e.target.value,
                              }))
                            }
                            placeholder={`${key} 값 입력`}
                            className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm outline-none"
                          />
                        ))}

                        {tableVarNames.map((key) => (
                          <div key={key} className="space-y-2 text-left">
                            <p className="text-sm font-medium text-[#1a2540]">{key} 표 데이터(JSON)</p>
                            <textarea
                              value={tableValues[key] ?? ""}
                              onChange={(e) =>
                                setTableValues((prev) => ({
                                  ...prev,
                                  [key]: e.target.value,
                                }))
                              }
                              rows={6}
                              className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm font-mono outline-none"
                              placeholder='[["품목","수량","금액"],["사과","2","3000"],["배","1","5000"]]'
                            />
                          </div>
                        ))}
                      </>
                    )}

                    <button
                      onClick={handleGenerateDocument}
                      disabled={generating}
                      className="w-full rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3 text-slate-700 hover:bg-[#f5f9ff] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {generating ? "문서 생성 중..." : "문서 생성 테스트"}
                    </button>

                    <button
                      onClick={reloadGeneratedFiles}
                      className="w-full rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3 text-slate-700 hover:bg-[#f5f9ff]"
                    >
                      생성 파일 새로고침
                    </button>
                  </div>
                </div>
              </section>

              <section className="grid gap-6 xl:grid-cols-2">
                <div className="rounded-[32px] border border-[#dbe7ff] bg-white ">
                  <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                    <div>
                      <h2 className="text-xl font-semibold text-[#162033]">템플릿</h2>
                      <p className="text-sm text-[#63708a]">업로드된 템플릿 목록</p>
                    </div>
                    <button className="rounded-2xl border border-[#d7e5ff] px-4 py-2 text-sm text-slate-700 hover:bg-[#f5f9ff]">
                      전체 보기
                    </button>
                  </div>
                  <div className="space-y-3 p-6">
                    {templates.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-[#d7e5ff] p-6 text-sm text-[#63708a]">
                        업로드된 템플릿이 없습니다.
                      </div>
                    ) : (
                      templates.slice(0, 6).map((item) => (
                        <div
                          key={item.templateId}
                          className="rounded-[24px] border border-[#dbe7ff] p-4 transition hover:border-[#1f69ef]/40 hover:shadow-[0_10px_26px_rgba(31,105,239,0.08)]"
                        >
                          <div className="flex items-center justify-between gap-4">
                            <div className="min-w-0">
                              <p className="truncate font-semibold text-[#1a2540]">{item.originalName}</p>
                              <p className="mt-1 text-sm text-[#63708a]">
                                업로드일 {formatDate(item.uploadedAt)} · {formatBytes(item.size)}
                              </p>
                            </div>
                            <button
                              onClick={() => handleOpenVars(item.templateId)}
                              className="inline-flex items-center gap-2 rounded-xl bg-[#1f69ef] px-3 py-1.5 text-sm font-semibold text-white hover:bg-[#1859ca]"
                            >
                              <Braces className="h-4 w-4 text-white" />
                              변수확인
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className="rounded-[32px] border border-[#dbe7ff] bg-white ">
                  <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                    <div>
                      <h2 className="text-xl font-semibold text-[#162033]">생성문서</h2>
                      <p className="text-sm text-[#63708a]">최근 생성된 문서</p>
                    </div>
                    <button
                      onClick={reloadGeneratedFiles}
                      className="rounded-2xl border border-[#d7e5ff] px-4 py-2 text-sm text-slate-700 hover:bg-[#f5f9ff]"
                    >
                      전체 보기
                    </button>
                  </div>
                  <div className="space-y-3 p-6">
                    {files.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-[#d7e5ff] p-6 text-sm text-[#63708a]">
                        생성된 문서가 없습니다.
                      </div>
                    ) : (
                      files.slice(0, 6).map((item) => (
                        <div
                          key={item.fileId}
                          className={`rounded-[24px] border p-4 transition hover:border-[#1f69ef]/40 hover:shadow-[0_10px_26px_rgba(31,105,239,0.08)] ${
                            highlightedFileId === item.fileId
                              ? "border-[#ffd400] bg-[#fffbe8] animate-pulse"
                              : "border-[#dbe7ff]"
                          }`}
                        >
                          <div className="flex items-center justify-between gap-4">
                            <div className="relative min-w-0">
                              {highlightedFileId === item.fileId ? (
                                <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-[#ffd400] shadow-sm" />
                              ) : null}

                              <p className="truncate font-semibold text-[#1a2540]">
                                {item.originalFilename}
                              </p>
                              <p className="mt-1 text-sm text-[#63708a]">
                                생성일 {formatDate(item.createdAt)} · {formatBytes(item.size)}
                              </p>
                            </div>

                            <div className="flex items-center gap-2">
                              <span
                                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                  item.status === "SUCCESS"
                                    ? "bg-[#eef4ff] text-[#1f69ef]"
                                    : "bg-red-50 text-red-700"
                                }`}
                              >
                                {item.status === "SUCCESS" ? "성공" : "실패"}
                              </span>
                              <button
                              onClick={() => handleDownloadFile(item)}
                              className="inline-flex items-center gap-2 rounded-xl bg-[#1f69ef] px-3 py-1.5 text-sm font-semibold text-white hover:bg-[#1859ca]"
                            >
                              <Download className="h-4 w-4 text-white" />
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

              <div className="rounded-[32px] border border-[#dbe7ff] bg-white ">
                <div className="flex flex-row items-start justify-between gap-4 p-6 pb-0">
                  <div>
                    <h2 className="text-xl font-semibold text-[#162033]">사용량 로그</h2>
                    <p className="text-sm text-[#63708a]">최근 API 사용 기록</p>
                  </div>
                  <button className="rounded-2xl border border-[#d7e5ff] px-4 py-2 text-sm text-slate-700 hover:bg-[#f5f9ff]">
                    전체 보기
                  </button>
                </div>
                <div className="p-6">
                  <div className="overflow-hidden rounded-[24px] border border-[#dbe7ff]">
                    <table className="min-w-full text-sm">
                      <thead className="bg-[#f3f8ff] text-[#45608d]">
                        <tr>
                          <th className="px-4 py-3 text-center font-semibold">실행</th>
                          <th className="px-4 py-3 text-center font-semibold">파일명</th>
                          <th className="px-4 py-3 text-center font-semibold">결과</th>
                          <th className="px-4 py-3 text-center font-semibold">생성시각</th>
                        </tr>
                      </thead>
                      <tbody>
                        {logs.length === 0 ? (
                          <tr className="border-t border-[#dbe7ff] bg-white">
                            <td colSpan={4} className="px-4 py-6 text-center text-[#63708a]">
                              사용 로그가 없습니다.
                            </td>
                          </tr>
                        ) : (
                          logs.slice(0, 10).map((item) => (
                            <tr key={item.id} className="border-t border-[#dbe7ff] bg-white">
                              <td className="px-4 py-3 text-center font-medium text-[#1a2540]">{item.actionType}</td>
                              <td className="px-4 py-3 text-center text-[#63708a]">{item.targetId || "-"}</td>
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
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#0f172a]/45 px-4">
          <div className="w-full max-w-lg rounded-[32px] border border-[#dbe7ff] bg-white p-6 shadow-[0_30px_80px_rgba(15,23,42,0.22)]">
            <div className="mb-4 flex items-start justify-between gap-4">
              <div>
                <h3 className="text-xl font-semibold text-[#162033]">Template Vars</h3>
                <p className="mt-1 text-sm text-[#63708a]">템플릿에서 추출된 placeholder 목록</p>
              </div>
              <button
                onClick={() => setVarsOpen(false)}
                className="rounded-xl border border-[#dbe7ff] p-2 hover:bg-[#f3f8ff]"
              >
                <X className="h-4 w-4 text-[#1f69ef]" />
              </button>
            </div>

            {varsLoading ? (
              <div className="rounded-2xl border border-[#d7e5ff] bg-[#f3f8ff] px-4 py-6 text-sm text-[#63708a]">
                Vars 불러오는 중...
              </div>
            ) : varsError ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-sm text-red-600">
                {varsError}
              </div>
            ) : varsData ? (
              <div className="space-y-4">
                <div className="rounded-2xl border border-[#d7e5ff] bg-[#f7fbff] px-4 py-3">
                  <p className="font-medium text-[#1a2540]">{varsData.originalName}</p>
                  <p className="mt-1 text-sm text-[#63708a]">templateId: {varsData.templateId}</p>
                </div>

                <div className="rounded-2xl border border-[#d7e5ff]">
                  <div className="border-b border-[#d7e5ff] px-4 py-3 text-sm font-medium text-[#1a2540]">
                    변수 개수: {varsData.varsCount}
                  </div>
                  <div className="max-h-72 space-y-2 overflow-y-auto p-4">
                    {varsData.vars.length === 0 ? (
                      <p className="text-sm text-[#63708a]">추출된 변수가 없습니다.</p>
                    ) : (
                      varsData.vars.map((item) => (
                        <div
                          key={item}
                          className="rounded-xl border border-[#dbe7ff] bg-white px-3 py-2 font-mono text-sm text-[#1a2540]"
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