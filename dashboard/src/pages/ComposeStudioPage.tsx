import type { PageType } from "../App"
import type { OverlayBox } from "../components/ComposePageOverlay"
import TopNav from "../components/TopNav"
import ComposePageParagraphLayer from "../components/ComposePageParagraphLayer"
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
  Table2,
  AlignLeft,
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
  x?: number | null
  y?: number | null
  width?: number | null
  height?: number | null
}

type TableTextStyle = {
  fontFamily?: string | null
  fontSize?: number | null
  bold?: boolean | null
  italic?: boolean | null
  underline?: boolean | null
  fontColor?: string | null
  align?: string | null
}

type BorderStyle = {
  color?: string | null
  width?: number | null
  lineStyle?: string | null
}

type ExtractedTableStyle = {
  headerTextStyle?: TableTextStyle | null
  bodyTextStyle?: TableTextStyle | null
  headerBackgroundColor?: string | null
  bodyBackgroundColor?: string | null
  headerOuterBorder?: BorderStyle | null
  headerInnerBorder?: BorderStyle | null
  bodyOuterBorder?: BorderStyle | null
  bodyInnerBorder?: BorderStyle | null
}

type TableItem = {
  tableId: string
  page: number | null
  rowCount: number | null
  colCount: number | null
  cellTexts: string[][]
  borderColor?: string | null
  borderWidth?: number | null
  backgroundColor?: string | null
  fontFamily?: string | null
  fontSize?: number | null
  bold?: boolean | null
  italic?: boolean | null
  fontColor?: string | null
  x?: number | null
  y?: number | null
  width?: number | null
  height?: number | null
  extractedStyle?: ExtractedTableStyle | null
}

type DocumentViewResponse = {
  paragraphs: ParagraphItem[]
  tables: TableItem[]
}

type PageBlockItem =
  | {
      kind: "paragraph"
      id: string
      page: number | null
      y: number
      labelText: string
      fontInfo: string
      paragraph: ParagraphItem
    }
  | {
      kind: "table"
      id: string
      page: number | null
      y: number
      labelText: string
      fontInfo: string
      table: TableItem
    }

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

const ROLE_OPTIONS = [
  { value: "heading1", label: "heading1" },
  { value: "heading2", label: "heading2" },
  { value: "heading3", label: "heading3" },
  { value: "body", label: "body" },
  { value: "caption", label: "caption" },
  { value: "table", label: "table" },
]

function fakePageCount(paragraphs: ParagraphItem[], tables: TableItem[]) {
  const pageValues = [
    ...paragraphs.map((p) => p.page),
    ...tables.map((t) => t.page),
  ].filter((v): v is number => typeof v === "number" && !Number.isNaN(v))

  if (pageValues.length === 0) return 1
  return Math.max(...pageValues)
}

function createAutoBox(index: number, paragraphId: string, label: string): OverlayBox {
  const row = index % 8
  const col = Math.floor(index / 8)

  const baseX = 36 + col * 260
  const baseY = 40 + row * 58

  return {
    id: `${paragraphId}-box`,
    x: baseX,
    y: baseY,
    width: 220,
    height: 42,
    label,
    paragraphId,
  }
}

function findBoxByParagraphId(boxes: OverlayBox[], paragraphId: string) {
  return boxes.find((box) => box.paragraphId === paragraphId) ?? null
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
  const [activePanel, setActivePanel] = useState<"paragraph" | "table">("paragraph")

  const [templateParagraphs, setTemplateParagraphs] = useState<ParagraphItem[]>([])
  const [sourceParagraphs, setSourceParagraphs] = useState<ParagraphItem[]>([])
  const [templateTables, setTemplateTables] = useState<TableItem[]>([])
  const [sourceTables, setSourceTables] = useState<TableItem[]>([])

  const [selectedTemplateParagraphId, setSelectedTemplateParagraphId] = useState("")
  const [selectedSourceParagraphId, setSelectedSourceParagraphId] = useState("")
  const [selectedTemplateTableId, setSelectedTemplateTableId] = useState("")
  const [selectedSourceTableId, setSelectedSourceTableId] = useState("")

  const [templateRole, setTemplateRole] = useState("heading1")
  const [sourceRole, setSourceRole] = useState("heading1")

  const [saving, setSaving] = useState(false)
  const [savedMessage, setSavedMessage] = useState("")
  const [applying, setApplying] = useState(false)
  const [applyResult, setApplyResult] = useState<any>(null)

  const [templateBoxes, setTemplateBoxes] = useState<OverlayBox[]>([])
  const [sourceBoxes, setSourceBoxes] = useState<OverlayBox[]>([])
  const [selectedBoxId, setSelectedBoxId] = useState<string | null>(null)

  const currentParagraphs = activeSide === "template" ? templateParagraphs : sourceParagraphs
  const currentTables = activeSide === "template" ? templateTables : sourceTables
  const currentBoxes = activeSide === "template" ? templateBoxes : sourceBoxes

  const currentSelectedId =
    activeSide === "template" ? selectedTemplateParagraphId : selectedSourceParagraphId

  const currentSelectedTableId =
    activeSide === "template" ? selectedTemplateTableId : selectedSourceTableId

  const currentRole = activeSide === "template" ? templateRole : sourceRole

  const currentPageCount = useMemo(
    () => fakePageCount(currentParagraphs, currentTables),
    [currentParagraphs, currentTables]
  )

  const visibleParagraphs = useMemo(() => {
    return currentParagraphs.filter((p) => p.page == null || p.page === currentPage)
  }, [currentParagraphs, currentPage])

  const visibleTables = useMemo(() => {
    return currentTables.filter((t) => t.page == null || t.page === currentPage)
  }, [currentTables, currentPage])

  const orderedPageBlocks = useMemo<PageBlockItem[]>(() => {
    const paragraphBlocks: PageBlockItem[] = visibleParagraphs.map((p) => ({
      kind: "paragraph",
      id: p.paragraphId,
      page: p.page,
      y: p.y ?? 0,
      labelText: p.text || "(빈 문단)",
      fontInfo: `${p.fontFamily ?? "-"} · ${p.fontSize ?? "-"}pt`,
      paragraph: p,
    }))

    const tableBlocks: PageBlockItem[] = visibleTables.map((t) => ({
      kind: "table",
      id: t.tableId,
      page: t.page,
      y: t.y ?? 0,
      labelText: t.cellTexts?.[0]?.join(" | ") || "(빈 표)",
      fontInfo: `${t.rowCount ?? 0}행 × ${t.colCount ?? 0}열`,
      table: t,
    }))

    return [...paragraphBlocks, ...tableBlocks].sort((a, b) => a.y - b.y)
  }, [visibleParagraphs, visibleTables])

  const filteredPanelBlocks = useMemo(() => {
    return orderedPageBlocks.filter((block) => {
      if (activePanel === "paragraph") return block.kind === "paragraph"
      return block.kind === "table"
    })
  }, [orderedPageBlocks, activePanel])

  useEffect(() => {
    setCurrentPage(1)
    setSelectedBoxId(null)
  }, [activeSide])

  useEffect(() => {
    if (!selectedBoxId) return

    if (activeSide === "template") {
      setTemplateBoxes((prev) =>
        prev.map((box) =>
          box.id === selectedBoxId ? { ...box, label: templateRole } : box
        )
      )
    } else {
      setSourceBoxes((prev) =>
        prev.map((box) =>
          box.id === selectedBoxId ? { ...box, label: sourceRole } : box
        )
      )
    }
  }, [templateRole, sourceRole, activeSide, selectedBoxId])

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
          `${API_BASE_URL}/api/v1/compose/document/${previewData.sessionId}?side=template`,
          { headers: { "X-API-Key": apiKey } }
        ),
        fetch(
          `${API_BASE_URL}/api/v1/compose/document/${previewData.sessionId}?side=source`,
          { headers: { "X-API-Key": apiKey } }
        ),
      ])

      if (!templateRes.ok) {
        throw new Error("template document 조회 실패")
      }
      if (!sourceRes.ok) {
        throw new Error("source document 조회 실패")
      }

      const templateData: DocumentViewResponse = await templateRes.json()
      const sourceData: DocumentViewResponse = await sourceRes.json()

      setTemplateParagraphs(templateData.paragraphs ?? [])
      setTemplateTables(templateData.tables ?? [])
      setSourceParagraphs(sourceData.paragraphs ?? [])
      setSourceTables(sourceData.tables ?? [])

      setSelectedTemplateParagraphId("")
      setSelectedSourceParagraphId("")
      setSelectedTemplateTableId("")
      setSelectedSourceTableId("")

      setTemplateBoxes([])
      setSourceBoxes([])
      setSelectedBoxId(null)
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

    const tableId =
      activeSide === "template" ? selectedTemplateTableId : selectedSourceTableId

    try {
      setSaving(true)
      setSavedMessage("")

      const body =
        activePanel === "table"
          ? {
              sessionId,
              side: activeSide,
              page: currentPage,
              label: currentRole,
              blockType: "table",
              tableId,
            }
          : {
              sessionId,
              side: activeSide,
              page: currentPage,
              label: currentRole,
              blockType: "paragraph",
              paragraphId,
            }

      if (activePanel === "table") {
        if (!tableId) {
          alert("표를 먼저 선택해주세요.")
          return
        }
      } else {
        if (!paragraphId) {
          alert("문단을 먼저 선택해주세요.")
          return
        }
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
      const savedTarget = data.blockType === "table" ? data.tableId : data.paragraphId

      setSavedMessage(
        `${data.side} · ${data.label} · ${data.blockType} · ${savedTarget} 저장 완료`
      )
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
        label: currentRole,
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
                  가상 페이지 렌더링 + 문단/표 선택
                </h1>
                <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                  HWPX를 직접 파싱해 문단과 표를 가상 페이지 위에 표시하는 단계입니다.
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
                가상 페이지 위에 문단과 표 박스를 함께 표시합니다
              </div>

              <ComposePageParagraphLayer
                paragraphs={visibleParagraphs}
                tables={visibleTables}
                selectedParagraphId={currentSelectedId || null}
                boxes={currentBoxes}
                selectedBoxId={selectedBoxId}
                onSelectParagraph={(paragraphId) => {
                  const existingBox = currentBoxes.find((box) => box.paragraphId === paragraphId)

                  if (activeSide === "template") {
                    setSelectedTemplateParagraphId(paragraphId)
                  } else {
                    setSelectedSourceParagraphId(paragraphId)
                  }

                  if (existingBox) {
                    setSelectedBoxId(existingBox.id)
                    return
                  }

                  const paragraph = visibleParagraphs.find((p) => p.paragraphId === paragraphId)
                  if (!paragraph) return

                  const autoBox = {
                    id: `${paragraphId}-box`,
                    x: paragraph.x ?? 40,
                    y: paragraph.y ?? 40,
                    width: paragraph.width ?? 420,
                    height: paragraph.height ?? 34,
                    label: currentRole,
                    paragraphId,
                  }

                  if (activeSide === "template") {
                    setTemplateBoxes((prev) => [...prev, autoBox])
                  } else {
                    setSourceBoxes((prev) => [...prev, autoBox])
                  }

                  setSelectedBoxId(autoBox.id)
                }}
                onSelectBox={(boxId) => {
                  setSelectedBoxId(boxId)

                  const clickedBox = currentBoxes.find((box) => box.id === boxId)
                  if (!clickedBox?.paragraphId) return

                  if (activeSide === "template") {
                    setSelectedTemplateParagraphId(clickedBox.paragraphId)
                  } else {
                    setSelectedSourceParagraphId(clickedBox.paragraphId)
                  }
                }}
              />
            </div>

            <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6">
              <div className="mb-4 flex items-center gap-2 text-lg font-semibold text-[#162033]">
                <Tag className="h-5 w-5 text-[#1f69ef]" />
                선택 패널
              </div>

              <div className="mb-4 flex items-center gap-2">
                <button
                  onClick={() => setActivePanel("paragraph")}
                  className={`inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm ${
                    activePanel === "paragraph"
                      ? "bg-[#1f69ef] text-white"
                      : "border border-[#d7e5ff] bg-white text-slate-700"
                  }`}
                >
                  <AlignLeft className="h-4 w-4" />
                  문단
                </button>

                <button
                  onClick={() => setActivePanel("table")}
                  className={`inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm ${
                    activePanel === "table"
                      ? "bg-[#1f69ef] text-white"
                      : "border border-[#d7e5ff] bg-white text-slate-700"
                  }`}
                >
                  <Table2 className="h-4 w-4" />
                  표
                </button>
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
                {filteredPanelBlocks.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-[#d7e5ff] px-4 py-8 text-center text-sm text-[#63708a]">
                    현재 페이지에서 {activePanel === "paragraph" ? "문단" : "표"}를 찾지 못했습니다.
                  </div>
                ) : (
                  filteredPanelBlocks.map((block) => {
                    if (block.kind === "paragraph") {
                      const p = block.paragraph
                      const selected = currentSelectedId === p.paragraphId

                      return (
                        <button
                          key={`p-${p.paragraphId}`}
                          type="button"
                          onClick={() => {
                            const currentBoxesForSide =
                              activeSide === "template" ? templateBoxes : sourceBoxes
                            const existingBox = findBoxByParagraphId(currentBoxesForSide, p.paragraphId)

                            if (activeSide === "template") {
                              setSelectedTemplateParagraphId(p.paragraphId)
                            } else {
                              setSelectedSourceParagraphId(p.paragraphId)
                            }

                            if (existingBox) {
                              setSelectedBoxId(existingBox.id)
                              return
                            }

                            const paragraphIndex = visibleParagraphs.findIndex(
                              (item) => item.paragraphId === p.paragraphId
                            )

                            const autoBox = {
                              id: `${p.paragraphId}-box`,
                              x: p.x ?? 40,
                              y: p.y ?? 40,
                              width: p.width ?? 420,
                              height: p.height ?? 34,
                              label: currentRole,
                              paragraphId: p.paragraphId,
                            }

                            if (activeSide === "template") {
                              setTemplateBoxes((prev) => [...prev, autoBox])
                            } else {
                              setSourceBoxes((prev) => [...prev, autoBox])
                            }

                            setSelectedBoxId(autoBox.id)
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
                            <span className="text-[11px] text-[#7a869f]">{block.fontInfo}</span>
                          </div>

                          <p className="line-clamp-3 text-sm font-medium text-[#1a2540]">
                            {block.labelText}
                          </p>
                        </button>
                      )
                    }

                    const t = block.table
                    const selected = currentSelectedTableId === t.tableId

                    return (
                      <button
                        key={`t-${t.tableId}`}
                        type="button"
                        onClick={() => {
                          if (activeSide === "template") {
                            setSelectedTemplateTableId(t.tableId)
                          } else {
                            setSelectedSourceTableId(t.tableId)
                          }

                          const tableBox: OverlayBox = {
                            id: `${t.tableId}-box`,
                            x: t.x ?? 56,
                            y: t.y ?? 80,
                            width: t.width ?? 420,
                            height: t.height ?? 120,
                            label: "table",
                          }

                          if (activeSide === "template") {
                            setTemplateBoxes((prev) => {
                              const exists = prev.find((b) => b.id === tableBox.id)
                              return exists ? prev : [...prev, tableBox]
                            })
                          } else {
                            setSourceBoxes((prev) => {
                              const exists = prev.find((b) => b.id === tableBox.id)
                              return exists ? prev : [...prev, tableBox]
                            })
                          }

                          setSelectedBoxId(tableBox.id)
                        }}
                        className={`w-full rounded-[22px] border p-4 text-left transition ${
                          selected
                            ? "border-[#1f69ef] bg-[#eef4ff] shadow-[0_12px_30px_rgba(31,105,239,0.10)]"
                            : "border-[#dbe7ff] bg-white hover:border-[#bcd2ff] hover:bg-[#f8fbff]"
                        }`}
                      >
                        <div className="mb-2 flex items-center justify-between gap-3">
                          <span className="rounded-full bg-white px-2.5 py-1 text-[11px] font-semibold text-[#1f69ef]">
                            {t.tableId}
                          </span>
                          <span className="text-[11px] text-[#7a869f]">{block.fontInfo}</span>
                        </div>

                        <p className="line-clamp-2 text-sm font-medium text-[#1a2540]">
                          {block.labelText}
                        </p>
                      </button>
                    )
                  })
                )}
              </div>

              <button
                onClick={handleSaveSelection}
                disabled={
                  activePanel === "paragraph"
                    ? !currentSelectedId || saving
                    : !currentSelectedTableId || saving
                }
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

              <div className="mt-4 rounded-2xl border border-[#e5ecff] bg-[#fafcff] px-4 py-3 text-sm text-[#4b5875]">
                <div>
                  현재 side: <span className="font-semibold">{activeSide}</span>
                </div>
                <div>
                  현재 panel: <span className="font-semibold">{activePanel}</span>
                </div>
                <div>
                  현재 role: <span className="font-semibold">{currentRole}</span>
                </div>
                <div>
                  현재 paragraph: <span className="font-semibold">{currentSelectedId || "-"}</span>
                </div>
                <div>
                  현재 table: <span className="font-semibold">{currentSelectedTableId || "-"}</span>
                </div>
                <div>
                  현재 overlay box: <span className="font-semibold">{selectedBoxId || "-"}</span>
                </div>
              </div>
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