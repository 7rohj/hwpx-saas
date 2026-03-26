import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import ComposePageOverlay, { type OverlayBox } from "../components/ComposePageOverlay"
import { useEffect, useMemo, useState } from "react"
import {
  FileUp,
  Layers3,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Tag,
  Sparkles,
  SquareDashedMousePointer,
} from "lucide-react"

type ComposeStudioPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

type PreviewResponse = {
  sessionId: string
  templateOriginalFilename?: string
  sourceOriginalFilename?: string
}

type ParagraphItem = {
  paragraphId: string
  page: number | null
  text: string
  fontSize: number | null
  bold: boolean | null
  italic: boolean | null
  underline: boolean | null
  color: string | null
  align: string | null
  spaceBefore: number | null
  spaceAfter: number | null
  lineSpacing: number | null
  singleLine: boolean | null
  paraPrIdRef?: string | null
  charPrIdRef?: string | null
  fontFamily?: string | null
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

const ROLE_OPTIONS = [
  { value: "heading1", label: "heading1" },
  { value: "heading2", label: "heading2" },
  { value: "heading3", label: "heading3" },
  { value: "body", label: "body" },
  { value: "caption", label: "caption" },
]

function fakePageCount(paragraphs: ParagraphItem[]) {
  const pages = paragraphs
    .map((p) => p.page)
    .filter((v): v is number => typeof v === "number" && !Number.isNaN(v))

  if (pages.length === 0) return 1
  return Math.max(...pages)
}

export default function ComposeStudioPage({
  onNavigate,
  onLogout,
}: ComposeStudioPageProps) {
  const [templateFile, setTemplateFile] = useState<File | null>(null)
  const [sourceFile, setSourceFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [sessionId, setSessionId] = useState("")
  const [error, setError] = useState("")

  const [activeSide, setActiveSide] = useState<"template" | "source">("template")
  const [currentPage, setCurrentPage] = useState(1)

  const [templateParagraphs, setTemplateParagraphs] = useState<ParagraphItem[]>([])
  const [sourceParagraphs, setSourceParagraphs] = useState<ParagraphItem[]>([])

  const [selectedTemplateParagraphId, setSelectedTemplateParagraphId] = useState("")
  const [selectedSourceParagraphId, setSelectedSourceParagraphId] = useState("")

  const [templateRole, setTemplateRole] = useState("heading1")
  const [sourceRole, setSourceRole] = useState("heading1")

  const [saving, setSaving] = useState(false)
  const [savedMessage, setSavedMessage] = useState("")
  const [applying, setApplying] = useState(false)
  const [applyResult, setApplyResult] = useState<any>(null)

  // 추가:
  // 지금 단계에서는 bbox가 없어서 박스를 프론트에서 임시 생성
  const [templateBoxes, setTemplateBoxes] = useState<OverlayBox[]>([])
  const [sourceBoxes, setSourceBoxes] = useState<OverlayBox[]>([])
  const [selectedBoxId, setSelectedBoxId] = useState<string | null>(null)

  const currentParagraphs = activeSide === "template" ? templateParagraphs : sourceParagraphs
  const currentSelectedId =
    activeSide === "template" ? selectedTemplateParagraphId : selectedSourceParagraphId
  const currentRole = activeSide === "template" ? templateRole : sourceRole
  const currentPageCount = useMemo(() => fakePageCount(currentParagraphs), [currentParagraphs])

  const currentBoxes = activeSide === "template" ? templateBoxes : sourceBoxes

  const visibleParagraphs = useMemo(() => {
    return currentParagraphs.filter((p) => {
      if (p.page == null) return true
      return p.page === currentPage
    })
  }, [currentParagraphs, currentPage])

  useEffect(() => {
    setCurrentPage(1)
    setSelectedBoxId(null)
  }, [activeSide])

  async function handlePreviewUpload() {
    if (!templateFile || !sourceFile) {
      alert("템플릿 파일과 source 파일을 모두 선택해주세요.")
      return
    }

    try {
      setUploading(true)
      setError("")
      setSavedMessage("")
      setApplyResult(null)

      const formData = new FormData()
      formData.append("templateFile", templateFile)
      formData.append("sourceFile", sourceFile)

      const previewRes = await fetch(`${API_BASE_URL}/api/v1/compose/preview`, {
        method: "POST",
        body: formData,
      })

      if (!previewRes.ok) {
        throw new Error("preview 업로드에 실패했습니다.")
      }

      const previewData: PreviewResponse = await previewRes.json()
      if (!previewData.sessionId) {
        throw new Error("sessionId를 받지 못했습니다.")
      }

      setSessionId(previewData.sessionId)

      const apiKey = localStorage.getItem("apiKey") ?? ""

      const [templateRes, sourceRes] = await Promise.all([
        fetch(
          `${API_BASE_URL}/api/v1/compose/paragraphs/${previewData.sessionId}?side=template&page=1`,
          {
            headers: { "X-API-Key": apiKey },
          }
        ),
        fetch(
          `${API_BASE_URL}/api/v1/compose/paragraphs/${previewData.sessionId}?side=source&page=1`,
          {
            headers: { "X-API-Key": apiKey },
          }
        ),
      ])

      if (!templateRes.ok) {
        throw new Error("template paragraph 조회 실패")
      }
      if (!sourceRes.ok) {
        throw new Error("source paragraph 조회 실패")
      }

      const templateData: ParagraphItem[] = await templateRes.json()
      const sourceData: ParagraphItem[] = await sourceRes.json()

      setTemplateParagraphs(templateData ?? [])
      setSourceParagraphs(sourceData ?? [])
      setSelectedTemplateParagraphId("")
      setSelectedSourceParagraphId("")

      // 추가:
      // 박스 임시 초기화
      setTemplateBoxes([])
      setSourceBoxes([])
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : "업로드 중 오류가 발생했습니다.")
    } finally {
      setUploading(false)
    }
  }

  async function handleSaveSelection() {
    if (!sessionId) {
      alert("먼저 preview 업로드를 해주세요.")
      return
    }

    const apiKey = localStorage.getItem("apiKey") ?? ""

    const paragraphId =
      activeSide === "template" ? selectedTemplateParagraphId : selectedSourceParagraphId

    if (!paragraphId) {
      alert("문단을 먼저 선택해주세요.")
      return
    }

    try {
      setSaving(true)
      setSavedMessage("")

      const body = {
        sessionId,
        side: activeSide,
        page: currentPage,
        label: currentRole,
        paragraphId,
      }

      const res = await fetch(`${API_BASE_URL}/api/v1/compose/selections`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-API-Key": apiKey,
        },
        body: JSON.stringify(body),
      })

      if (!res.ok) {
        throw new Error("선택 저장에 실패했습니다.")
      }

      const data = await res.json()
      setSavedMessage(`${data.side} · ${data.label} · ${data.paragraphId} 저장 완료`)
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "선택 저장 실패")
    } finally {
      setSaving(false)
    }
  }

  async function handleApply() {
    if (!sessionId) {
      alert("먼저 preview 업로드를 해주세요.")
      return
    }

    const apiKey = localStorage.getItem("apiKey") ?? ""

    try {
      setApplying(true)
      setApplyResult(null)

      const body = {
        sessionId,
        label: "heading1",
        fontPolicy: "original",
      }

      const res = await fetch(`${API_BASE_URL}/api/v1/compose/apply`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-API-Key": apiKey,
        },
        body: JSON.stringify(body),
      })

      if (!res.ok) {
        throw new Error("apply 실패")
      }

      const data = await res.json()
      setApplyResult(data)
      alert("apply 완료!")
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "apply 실패")
    } finally {
      setApplying(false)
    }
  }

  function createOverlayBox(box: Omit<OverlayBox, "id">) {
    const newBox: OverlayBox = {
      id: `${activeSide}-box-${Date.now()}`,
      ...box,
      label: currentRole,
    }

    if (activeSide === "template") {
      setTemplateBoxes((prev) => [...prev, newBox])
    } else {
      setSourceBoxes((prev) => [...prev, newBox])
    }

    setSelectedBoxId(newBox.id)
  }

  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="composeStudio" onLogout={onLogout} />

      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 rounded-[32px] border border-[#cfe0ff] bg-white/85 p-6 backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                  <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
                  Compose Studio
                </div>
                <h1 className="text-3xl font-bold tracking-tight text-[#162033] sm:text-4xl">
                  페이지 오버레이 + 문단 선택
                </h1>
                <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                  지금은 문단 직접 선택이 메인이고, 오버레이 박스 UX를 같이 붙이는 단계입니다.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <button
                  onClick={handlePreviewUpload}
                  disabled={uploading}
                  className="inline-flex items-center gap-2 rounded-2xl bg-[#1f69ef] px-5 py-3 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(31,105,239,0.22)] hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <FileUp className="h-4 w-4" />
                  {uploading ? "불러오는 중..." : "파일 불러오기"}
                </button>

                <button
                  onClick={handleApply}
                  disabled={!sessionId || applying}
                  className="inline-flex items-center gap-2 rounded-2xl border border-[#d7e5ff] bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-[#f5f9ff] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Sparkles className="h-4 w-4" />
                  {applying ? "적용 중..." : "apply 실행"}
                </button>
              </div>
            </div>
          </header>

          {error ? (
            <div className="mb-6 rounded-[24px] border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600">
              {error}
            </div>
          ) : null}

          <section className="mb-6 grid gap-4 lg:grid-cols-2">
            <div className="rounded-[28px] border border-[#dbe7ff] bg-white p-5">
              <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-[#1f69ef]">
                <Layers3 className="h-4 w-4" />
                Template 파일
              </div>
              <input
                type="file"
                accept=".hwpx"
                onChange={(e) => setTemplateFile(e.target.files?.[0] ?? null)}
                className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm"
              />
              <p className="mt-2 text-sm text-[#63708a]">
                {templateFile ? templateFile.name : "선택된 파일 없음"}
              </p>
            </div>

            <div className="rounded-[28px] border border-[#dbe7ff] bg-white p-5">
              <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-[#1f69ef]">
                <Layers3 className="h-4 w-4" />
                Source 파일
              </div>
              <input
                type="file"
                accept=".hwpx"
                onChange={(e) => setSourceFile(e.target.files?.[0] ?? null)}
                className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm"
              />
              <p className="mt-2 text-sm text-[#63708a]">
                {sourceFile ? sourceFile.name : "선택된 파일 없음"}
              </p>
            </div>
          </section>

          <section className="grid gap-6 lg:grid-cols-[1.5fr_0.95fr]">
            <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setActiveSide("template")}
                    className={`rounded-2xl px-4 py-2 text-sm font-medium transition ${
                      activeSide === "template"
                        ? "bg-[#1f69ef] text-white"
                        : "border border-[#d7e5ff] bg-white text-slate-700 hover:bg-[#f5f9ff]"
                    }`}
                  >
                    Template
                  </button>

                  <button
                    onClick={() => setActiveSide("source")}
                    className={`rounded-2xl px-4 py-2 text-sm font-medium transition ${
                      activeSide === "source"
                        ? "bg-[#1f69ef] text-white"
                        : "border border-[#d7e5ff] bg-white text-slate-700 hover:bg-[#f5f9ff]"
                    }`}
                  >
                    Source
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                    className="rounded-xl border border-[#d7e5ff] p-2 hover:bg-[#f5f9ff]"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>

                  <div className="rounded-xl border border-[#d7e5ff] px-4 py-2 text-sm font-medium text-slate-700">
                    page {currentPage} / {currentPageCount}
                  </div>

                  <button
                    onClick={() => setCurrentPage((prev) => Math.min(currentPageCount, prev + 1))}
                    className="rounded-xl border border-[#d7e5ff] p-2 hover:bg-[#f5f9ff]"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>

              <div className="mb-3 flex items-center gap-2 text-xs font-semibold text-[#1f69ef]">
                <SquareDashedMousePointer className="h-4 w-4" />
                드래그해서 임시 박스를 만들어 UX를 먼저 확인할 수 있어요
              </div>

              <ComposePageOverlay
                boxes={currentBoxes}
                selectedBoxId={selectedBoxId}
                onSelectBox={setSelectedBoxId}
                onCreateBox={createOverlayBox}
              />
            </div>

            <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6">
              <div className="mb-4 flex items-center gap-2 text-lg font-semibold text-[#162033]">
                <Tag className="h-5 w-5 text-[#1f69ef]" />
                문단 선택
              </div>

              <div className="mb-4">
                <label className="mb-2 block text-sm font-medium text-[#1a2540]">
                  역할(label)
                </label>
                <select
                  value={currentRole}
                  onChange={(e) => {
                    if (activeSide === "template") setTemplateRole(e.target.value)
                    else setSourceRole(e.target.value)
                  }}
                  className="w-full rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3 text-sm text-[#1a2540] outline-none"
                >
                  {ROLE_OPTIONS.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="mb-4 max-h-[420px] space-y-3 overflow-y-auto pr-1">
                {visibleParagraphs.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-[#d7e5ff] px-4 py-8 text-center text-sm text-[#63708a]">
                    현재 페이지에서 문단을 찾지 못했습니다.
                  </div>
                ) : (
                  visibleParagraphs.map((p) => {
                    const selected = currentSelectedId === p.paragraphId

                    return (
                      <button
                        key={p.paragraphId}
                        type="button"
                        onClick={() => {
                          if (activeSide === "template") setSelectedTemplateParagraphId(p.paragraphId)
                          else setSelectedSourceParagraphId(p.paragraphId)
                        }}
                        className={`w-full rounded-[22px] border p-4 text-left transition ${
                          selected
                            ? "border-[#1f69ef] bg-[#eef4ff] shadow-[0_12px_30px_rgba(31,105,239,0.10)]"
                            : "border-[#dbe7ff] bg-white hover:border-[#bcd2ff] hover:bg-[#f8fbff]"
                        }`}
                      >
                        <div className="mb-2 flex items-center justify-between gap-3">
                          <span className="rounded-full bg-white px-2.5 py-1 text-[11px] font-semibold text-[#1f69ef]">
                            {p.paragraphId}
                          </span>
                          <span className="text-[11px] text-[#7a869f]">
                            {p.fontFamily ?? "-"} · {p.fontSize ?? "-"}pt
                          </span>
                        </div>

                        <p className="line-clamp-3 text-sm font-medium text-[#1a2540]">
                          {p.text || "(빈 문단)"}
                        </p>
                      </button>
                    )
                  })
                )}
              </div>

              <button
                onClick={handleSaveSelection}
                disabled={!currentSelectedId || saving}
                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#1f69ef] px-4 py-3 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(31,105,239,0.22)] hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-60"
              >
                <CheckCircle2 className="h-4 w-4" />
                {saving ? "저장 중..." : "선택 저장"}
              </button>

              {savedMessage ? (
                <div className="mt-4 rounded-2xl border border-[#d7e5ff] bg-[#f3f8ff] px-4 py-3 text-sm text-[#1f69ef]">
                  {savedMessage}
                </div>
              ) : null}
            </div>
          </section>

          {applyResult ? (
            <section className="mt-6 rounded-[32px] border border-[#dbe7ff] bg-white p-6">
              <h2 className="text-xl font-semibold text-[#162033]">apply 결과</h2>
              <pre className="mt-4 overflow-x-auto rounded-2xl bg-[#0f172a] p-4 text-xs text-slate-100">
                {JSON.stringify(applyResult, null, 2)}
              </pre>
            </section>
          ) : null}
        </div>
      </div>
    </>
  )
}