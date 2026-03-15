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

function isTableVarName(name: string) {
  const lower = name.toLowerCase()
  return (
    lower.includes("table") ||
    lower.includes("rows") ||
    lower.includes("items")
  )
}

const panelClass =
  "border border-[#2f4f2f] bg-[#0a0f0a] text-[#b7ffb7] shadow-[0_0_0_1px_rgba(0,0,0,0.35)]"

const panelHeaderClass =
  "border-b border-[#2f4f2f] bg-[#121812] px-5 py-3"

const inputClass =
  "w-full border border-[#2f4f2f] bg-black px-3 py-3 font-mono text-sm text-[#d8ffd8] outline-none placeholder:text-[#4d6b4d]"

const buttonClass =
  "border border-[#2f4f2f] bg-[#111611] px-4 py-3 font-mono text-sm text-[#b7ffb7] transition hover:bg-[#162016] hover:text-[#d8ffd8]"

const activeButtonClass =
  "border border-[#7ee787] bg-[#16301b] px-4 py-3 font-mono text-sm font-bold tracking-wide text-[#d8ffd8] transition hover:bg-[#1c3a21]"

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
            next[key] =
              prev[key] ?? '[["항목","값"],["예시1","100"],["예시2","200"]]'
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

    const hasEmptyTextValue = textVarNames.some(
      (key) => !String(textValues[key] ?? "").trim()
    )

    if (hasEmptyTextValue) {
      alert("텍스트 변수 값을 모두 입력해주세요.")
      return
    }

    const hasEmptyTableValue = tableVarNames.some(
      (key) => !String(tableValues[key] ?? "").trim()
    )

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
      <div className="min-h-screen bg-[#050805] text-[#b7ffb7]">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className={panelClass}>
            <div className={panelHeaderClass}>
              <p className="font-mono text-sm font-bold text-[#d8ffd8]">BOOT_SEQUENCE</p>
            </div>
            <div className="p-6 font-mono text-sm text-[#8fd48f]">
              LOADING_DASHBOARD_DATA...
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#050805] text-[#b7ffb7]">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="border border-red-800 bg-[#120909] text-red-300 shadow-[0_0_0_1px_rgba(0,0,0,0.35)]">
            <div className="border-b border-red-900 bg-[#1a0d0d] px-5 py-3">
              <p className="font-mono text-sm font-bold">SYSTEM_ERROR</p>
            </div>
            <div className="p-6 font-mono text-sm">
              <p>{error}</p>
              <p className="mt-3 text-xs text-red-400">
                localStorage에 userEmail / apiKey가 저장되어 있는지도 확인해보세요.
              </p>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className="min-h-screen bg-[#050805] text-[#b7ffb7]">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 border border-[#2f4f2f] bg-[#0a0f0a] shadow-[0_0_0_1px_rgba(0,0,0,0.35)]">
            <div className="border-b border-[#2f4f2f] bg-[#121812] px-5 py-3">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <div className="mb-2 inline-flex items-center gap-2 border border-[#2f4f2f] bg-[#0d130d] px-3 py-1 font-mono text-xs text-[#7ee787]">
                    <span className="h-2 w-2 bg-[#7ee787]" />
                    AUTOHWPX_TERMINAL
                  </div>
                  <h1 className="font-mono text-2xl font-bold tracking-wide text-[#d8ffd8] sm:text-3xl">
                    DASHBOARD.CONSOLE
                  </h1>
                  <p className="mt-2 font-mono text-xs text-[#7ee787]">
                    api-key / templates / generation / files
                  </p>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <div className="flex items-center gap-2 border border-[#2f4f2f] bg-[#0d130d] px-4 py-3 font-mono text-xs text-[#8fd48f]">
                    <Search className="h-4 w-4" />
                    SEARCH_MODULE_PENDING
                  </div>
                  <div className="border border-[#2f4f2f] bg-[#0d130d] px-4 py-3 font-mono text-xs text-[#b7ffb7]">
                    <p className="font-bold text-[#d8ffd8]">{email}</p>
                    <p className="mt-1 text-[#7ee787]">PLAN: STARTER</p>
                  </div>
                </div>
              </div>
            </div>
          </header>

          <div className="grid gap-6 lg:grid-cols-12">
            <aside className="space-y-6 lg:col-span-3">
              <div className={panelClass}>
                <div className={panelHeaderClass}>
                  <p className="font-mono text-sm font-bold tracking-wide text-[#d8ffd8]">
                    MENU.SYS
                  </p>
                </div>
                <div className="space-y-2 p-4">
                  <button className="flex w-full items-center gap-3 border border-[#7ee787] bg-[#16301b] px-4 py-3 text-left font-mono text-sm font-bold text-[#d8ffd8]">
                    <LayoutDashboard className="h-4 w-4" />
                    DASHBOARD
                  </button>
                  <button className={`${buttonClass} flex w-full items-center gap-3 text-left`}>
                    <FolderOpen className="h-4 w-4" />
                    TEMPLATES
                  </button>
                  <button className={`${buttonClass} flex w-full items-center gap-3 text-left`}>
                    <FileText className="h-4 w-4" />
                    GENERATED_FILES
                  </button>
                  <button className={`${buttonClass} flex w-full items-center gap-3 text-left`}>
                    <Activity className="h-4 w-4" />
                    USAGE_LOGS
                  </button>
                  <button className={`${buttonClass} flex w-full items-center gap-3 text-left`}>
                    <CreditCard className="h-4 w-4" />
                    BILLING
                  </button>
                </div>
              </div>

              <div className={panelClass}>
                <div className={panelHeaderClass}>
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <h2 className="font-mono text-sm font-bold tracking-wide text-[#d8ffd8]">
                        API_KEY.DAT
                      </h2>
                      <p className="mt-1 font-mono text-[11px] text-[#7ee787]">
                        auth / upload / generate / dashboard
                      </p>
                    </div>
                    <button
                      onClick={handleRegenerateApiKey}
                      disabled={refreshingKey}
                      className="border border-[#2f4f2f] bg-[#111611] px-3 py-2 font-mono text-xs text-[#b7ffb7] hover:bg-[#162016] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <span className="inline-flex items-center gap-2">
                        <RefreshCw className={`h-3.5 w-3.5 ${refreshingKey ? "animate-spin" : ""}`} />
                        REISSUE
                      </span>
                    </button>
                  </div>
                </div>
                <div className="p-5">
                  <div className="border border-[#2f4f2f] bg-black p-4 font-mono text-xs leading-6 text-[#7ee787] break-all">
                    {apiKey || "API KEY 없음"}
                  </div>
                </div>
              </div>
            </aside>

            <main className="space-y-6 lg:col-span-9">
              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  ["TEMPLATES", String(templates.length)],
                  ["GENERATED_FILES", String(files.length)],
                  ["THIS_MONTH_USAGE", String(thisMonthUsage)],
                  ["PLAN", "STARTER"],
                ].map(([label, value]) => (
                  <div key={label} className={panelClass}>
                    <div className={panelHeaderClass}>
                      <p className="font-mono text-xs text-[#7ee787]">{label}</p>
                    </div>
                    <div className="p-5">
                      <p className="font-mono text-3xl font-bold tracking-tight text-[#d8ffd8]">
                        {value}
                      </p>
                    </div>
                  </div>
                ))}
              </section>

              <section>
                <div className={panelClass}>
                  <div className={panelHeaderClass}>
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <h2 className="font-mono text-lg font-bold tracking-wide text-[#d8ffd8]">
                          QUICK_ACTIONS.EXE
                        </h2>
                        <p className="mt-1 font-mono text-xs text-[#7ee787]">
                          upload / generate / refresh
                        </p>
                      </div>
                      <div className="font-mono text-[11px] text-[#5ea85e]">
                        STATUS: READY
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4 p-5 font-mono">
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

                    <div className="grid grid-cols-1 gap-3 xl:grid-cols-2">
                      <button
                        onClick={() => fileInputRef.current?.click()}
                        disabled={uploading}
                        className={`${buttonClass} text-left disabled:cursor-not-allowed disabled:opacity-50`}
                      >
                        <span className="flex items-center gap-2">
                          <Upload className="h-4 w-4" />
                          {uploading ? "UPLOAD_IN_PROGRESS..." : "UPLOAD_TEMPLATE"}
                        </span>
                      </button>

                      <button
                        onClick={reloadGeneratedFiles}
                        className={`${buttonClass} text-left`}
                      >
                        REFRESH_GENERATED_FILES
                      </button>
                    </div>

                    <div className="border border-[#233223] bg-[#0d130d] p-3">
                      <label className="mb-2 block text-xs uppercase tracking-wider text-[#7ee787]">
                        TEMPLATE_TARGET
                      </label>
                      <select
                        value={selectedTemplateId}
                        onChange={(e) => setSelectedTemplateId(e.target.value)}
                        className={inputClass}
                      >
                        <option value="">SELECT_TEMPLATE</option>
                        {templates.map((item) => (
                          <option key={item.templateId} value={item.templateId}>
                            {item.originalName}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="grid grid-cols-2 gap-2">
                      <button
                        type="button"
                        onClick={() => setInputMode("vars")}
                        className={`border px-4 py-2 font-mono text-xs tracking-wide transition ${
                          inputMode === "vars"
                            ? "border-[#7ee787] bg-[#16301b] text-[#d8ffd8]"
                            : "border-[#2f4f2f] bg-[#111611] text-[#8fd48f] hover:bg-[#162016]"
                        }`}
                      >
                        MODE_01 / VARS
                      </button>

                      <button
                        type="button"
                        onClick={() => setInputMode("json")}
                        className={`border px-4 py-2 font-mono text-xs tracking-wide transition ${
                          inputMode === "json"
                            ? "border-[#7ee787] bg-[#16301b] text-[#d8ffd8]"
                            : "border-[#2f4f2f] bg-[#111611] text-[#8fd48f] hover:bg-[#162016]"
                        }`}
                      >
                        MODE_02 / JSON
                      </button>
                    </div>

                    {inputMode === "json" ? (
                      <div className="space-y-2 text-left">
                        <p className="text-xs uppercase tracking-wider text-[#7ee787]">
                          PAYLOAD.JSON
                        </p>
                        <textarea
                          value={jsonPayload}
                          onChange={(e) => setJsonPayload(e.target.value)}
                          rows={20}
                          className="w-full border border-[#2f4f2f] bg-black px-3 py-3 font-mono text-sm leading-6 text-[#b7ffb7] outline-none placeholder:text-[#4d6b4d]"
                          placeholder="문서 생성용 JSON을 입력하세요."
                        />
                      </div>
                    ) : generateVarsLoading ? (
                      <div className="border border-[#2f4f2f] bg-[#111611] px-4 py-3 font-mono text-sm text-[#8fd48f]">
                        LOADING_TEMPLATE_VARIABLES...
                      </div>
                    ) : (
                      <div className="space-y-3">
                        {textVarNames.length === 0 && tableVarNames.length === 0 ? (
                          <div className="border border-dashed border-[#2f4f2f] bg-[#111611] px-4 py-3 font-mono text-sm text-[#8fd48f]">
                            NO_INPUT_VARIABLES_FOUND
                          </div>
                        ) : null}

                        {textVarNames.map((key) => (
                          <div key={key} className="border border-[#233223] bg-[#0d130d] p-3">
                            <label className="mb-2 block text-xs uppercase tracking-wider text-[#7ee787]">
                              {key}
                            </label>
                            <input
                              type="text"
                              value={textValues[key] ?? ""}
                              onChange={(e) =>
                                setTextValues((prev) => ({
                                  ...prev,
                                  [key]: e.target.value,
                                }))
                              }
                              placeholder={`${key}_VALUE`}
                              className={inputClass}
                            />
                          </div>
                        ))}

                        {tableVarNames.map((key) => (
                          <div key={key} className="border border-[#233223] bg-[#0d130d] p-3">
                            <div className="mb-2 flex items-center justify-between gap-3">
                              <label className="block text-xs uppercase tracking-wider text-[#7ee787]">
                                {key}
                              </label>
                              <span className="text-[11px] text-[#5ea85e]">TABLE_JSON</span>
                            </div>
                            <textarea
                              value={tableValues[key] ?? ""}
                              onChange={(e) =>
                                setTableValues((prev) => ({
                                  ...prev,
                                  [key]: e.target.value,
                                }))
                              }
                              rows={7}
                              className="w-full border border-[#2f4f2f] bg-black px-3 py-3 font-mono text-sm leading-6 text-[#b7ffb7] outline-none placeholder:text-[#4d6b4d]"
                              placeholder='[["품목","수량","금액"],["사과","2","3000"],["배","1","5000"]]'
                            />
                          </div>
                        ))}
                      </div>
                    )}

                    <div className="grid grid-cols-1 gap-3 xl:grid-cols-2">
                      <button
                        onClick={handleGenerateDocument}
                        disabled={generating}
                        className={`${activeButtonClass} disabled:cursor-not-allowed disabled:opacity-50`}
                      >
                        {generating ? "GENERATING_DOCUMENT..." : "RUN_GENERATE_TEST"}
                      </button>

                      <div className="border border-[#2f4f2f] bg-[#0d130d] px-4 py-3 font-mono text-xs text-[#7ee787]">
                        <div>$ mode: {inputMode}</div>
                        <div>$ template: {selectedTemplateId || "none"}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

              <section className="grid gap-6 xl:grid-cols-2">
                <div className={panelClass}>
                  <div className={panelHeaderClass}>
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <h2 className="font-mono text-sm font-bold tracking-wide text-[#d8ffd8]">
                          TEMPLATES.DIR
                        </h2>
                        <p className="mt-1 font-mono text-[11px] text-[#7ee787]">
                          uploaded template list
                        </p>
                      </div>
                      <button className="border border-[#2f4f2f] bg-[#111611] px-3 py-2 font-mono text-xs text-[#b7ffb7] hover:bg-[#162016]">
                        VIEW_ALL
                      </button>
                    </div>
                  </div>
                  <div className="space-y-3 p-5">
                    {templates.length === 0 ? (
                      <div className="border border-dashed border-[#2f4f2f] bg-[#111611] p-5 font-mono text-sm text-[#8fd48f]">
                        NO_TEMPLATES_FOUND
                      </div>
                    ) : (
                      templates.slice(0, 6).map((item) => (
                        <div
                          key={item.templateId}
                          className="border border-[#2f4f2f] bg-[#0d130d] p-4 transition hover:bg-[#101810]"
                        >
                          <div className="flex items-center justify-between gap-4">
                            <div className="min-w-0">
                              <p className="truncate font-mono text-sm font-bold text-[#d8ffd8]">
                                {item.originalName}
                              </p>
                              <p className="mt-1 font-mono text-xs text-[#7ee787]">
                                upload: {formatDate(item.uploadedAt)} / size: {formatBytes(item.size)}
                              </p>
                            </div>
                            <button
                              onClick={() => handleOpenVars(item.templateId)}
                              className="border border-[#2f4f2f] bg-[#111611] px-3 py-2 font-mono text-xs text-[#b7ffb7] hover:bg-[#162016]"
                            >
                              <span className="inline-flex items-center gap-2">
                                <Braces className="h-4 w-4" />
                                VARS
                              </span>
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className={panelClass}>
                  <div className={panelHeaderClass}>
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <h2 className="font-mono text-sm font-bold tracking-wide text-[#d8ffd8]">
                          GENERATED_FILES.OUT
                        </h2>
                        <p className="mt-1 font-mono text-[11px] text-[#7ee787]">
                          recently generated documents
                        </p>
                      </div>
                      <button
                        onClick={reloadGeneratedFiles}
                        className="border border-[#2f4f2f] bg-[#111611] px-3 py-2 font-mono text-xs text-[#b7ffb7] hover:bg-[#162016]"
                      >
                        VIEW_ALL
                      </button>
                    </div>
                  </div>
                  <div className="space-y-3 p-5">
                    {files.length === 0 ? (
                      <div className="border border-dashed border-[#2f4f2f] bg-[#111611] p-5 font-mono text-sm text-[#8fd48f]">
                        NO_GENERATED_FILES
                      </div>
                    ) : (
                      files.slice(0, 6).map((item) => (
                        <div
                          key={item.fileId}
                          className={`border p-4 transition ${
                            highlightedFileId === item.fileId
                              ? "border-red-500 bg-red-100/10 animate-pulse"
                              : "border-[#2f4f2f] bg-[#0d130d] hover:bg-[#101810]"
                          }`}
                        >
                          <div className="flex items-center justify-between gap-4">
                            <div className="relative min-w-0">
                              {highlightedFileId === item.fileId ? (
                                <span className="absolute -right-1 -top-1 h-3 w-3 bg-red-500 shadow-sm" />
                              ) : null}

                              <p className="truncate font-mono text-sm font-bold text-[#d8ffd8]">
                                {item.originalFilename}
                              </p>
                              <p className="mt-1 font-mono text-xs text-[#7ee787]">
                                created: {formatDate(item.createdAt)} / size: {formatBytes(item.size)}
                              </p>
                            </div>

                            <div className="flex items-center gap-2">
                              <span className="border border-[#2f4f2f] bg-[#111611] px-3 py-1 font-mono text-[11px] text-[#7ee787]">
                                {item.status}
                              </span>
                              <button
                                onClick={() => handleDownloadFile(item)}
                                className="border border-[#2f4f2f] bg-[#111611] px-3 py-2 font-mono text-xs text-[#b7ffb7] hover:bg-[#162016]"
                              >
                                <span className="inline-flex items-center gap-2">
                                  <Download className="h-4 w-4" />
                                  DOWNLOAD
                                </span>
                              </button>
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </section>

              <div className={panelClass}>
                <div className={panelHeaderClass}>
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <h2 className="font-mono text-sm font-bold tracking-wide text-[#d8ffd8]">
                        USAGE_LOGS.TXT
                      </h2>
                      <p className="mt-1 font-mono text-[11px] text-[#7ee787]">
                        recent API records
                      </p>
                    </div>
                    <button className="border border-[#2f4f2f] bg-[#111611] px-3 py-2 font-mono text-xs text-[#b7ffb7] hover:bg-[#162016]">
                      VIEW_ALL
                    </button>
                  </div>
                </div>
                <div className="p-5">
                  <div className="overflow-hidden border border-[#2f4f2f]">
                    <table className="min-w-full font-mono text-xs">
                      <thead className="bg-[#121812] text-[#7ee787]">
                        <tr>
                          <th className="border-b border-[#2f4f2f] px-4 py-3 text-left font-bold">ACTION</th>
                          <th className="border-b border-[#2f4f2f] px-4 py-3 text-left font-bold">TARGET</th>
                          <th className="border-b border-[#2f4f2f] px-4 py-3 text-left font-bold">RESULT</th>
                          <th className="border-b border-[#2f4f2f] px-4 py-3 text-left font-bold">TIME</th>
                        </tr>
                      </thead>
                      <tbody>
                        {logs.length === 0 ? (
                          <tr className="bg-[#0d130d]">
                            <td colSpan={4} className="px-4 py-6 text-center text-[#8fd48f]">
                              NO_USAGE_LOGS
                            </td>
                          </tr>
                        ) : (
                          logs.slice(0, 10).map((item) => (
                            <tr key={item.id} className="border-t border-[#2f4f2f] bg-[#0d130d]">
                              <td className="px-4 py-3 text-[#d8ffd8]">{item.actionType}</td>
                              <td className="px-4 py-3 text-[#8fd48f]">{item.targetId || "-"}</td>
                              <td className="px-4 py-3">
                                <span
                                  className={`border px-3 py-1 font-mono text-[11px] ${
                                    item.success
                                      ? "border-[#2f4f2f] bg-[#111611] text-[#7ee787]"
                                      : "border-red-800 bg-[#1a0d0d] text-red-300"
                                  }`}
                                >
                                  {item.success ? "SUCCESS" : "FAIL"}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-[#8fd48f]">{formatDateTime(item.createdAt)}</td>
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
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 px-4">
          <div className="w-full max-w-lg border border-[#2f4f2f] bg-[#0a0f0a] text-[#b7ffb7] shadow-[0_0_0_1px_rgba(0,0,0,0.35)]">
            <div className="flex items-start justify-between gap-4 border-b border-[#2f4f2f] bg-[#121812] px-5 py-3">
              <div>
                <h3 className="font-mono text-lg font-bold text-[#d8ffd8]">
                  TEMPLATE_VARS.VIEW
                </h3>
                <p className="mt-1 font-mono text-xs text-[#7ee787]">
                  extracted placeholder list
                </p>
              </div>
              <button
                onClick={() => setVarsOpen(false)}
                className="border border-[#2f4f2f] bg-[#111611] p-2 text-[#b7ffb7] hover:bg-[#162016]"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {varsLoading ? (
              <div className="p-5 font-mono text-sm text-[#8fd48f]">
                LOADING_VARS...
              </div>
            ) : varsError ? (
              <div className="border-t border-red-900 bg-[#1a0d0d] p-5 font-mono text-sm text-red-300">
                {varsError}
              </div>
            ) : varsData ? (
              <div className="space-y-4 p-5">
                <div className="border border-[#2f4f2f] bg-[#0d130d] px-4 py-3">
                  <p className="font-mono text-sm font-bold text-[#d8ffd8]">{varsData.originalName}</p>
                  <p className="mt-1 font-mono text-xs text-[#7ee787]">
                    templateId: {varsData.templateId}
                  </p>
                </div>

                <div className="border border-[#2f4f2f]">
                  <div className="border-b border-[#2f4f2f] bg-[#121812] px-4 py-3 font-mono text-xs font-bold text-[#7ee787]">
                    VAR_COUNT: {varsData.varsCount}
                  </div>
                  <div className="max-h-72 space-y-2 overflow-y-auto bg-[#0d130d] p-4">
                    {varsData.vars.length === 0 ? (
                      <p className="font-mono text-sm text-[#8fd48f]">NO_VARS_FOUND</p>
                    ) : (
                      varsData.vars.map((item) => (
                        <div
                          key={item}
                          className="border border-[#2f4f2f] bg-black px-3 py-2 font-mono text-sm text-[#b7ffb7]"
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