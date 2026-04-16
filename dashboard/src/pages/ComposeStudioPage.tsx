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
  Clock3,
  Search,
  PanelLeft,
  X,
  History,
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
  tableKind?: string | null
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

type SavedSelectionHistoryItem = {
  id: string
  sessionId: string
  side: "template" | "source"
  page: number
  label: string
  blockType: "paragraph" | "table"
  paragraphId?: string
  tableId?: string
  savedAt: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

const ROLE_OPTIONS = [
  { value: "heading1", label: "heading1" },
  { value: "heading2", label: "heading2" },
  { value: "heading3", label: "heading3" },
  { value: "body", label: "body" },
  { value: "icon", label: "icon" },
  { value: "caption", label: "caption" },
  { value: "oneline_box", label: "oneline_box" },
  { value: "table", label: "table" },
]

function shimmerStyle(delay = "0s") {
  return {
    background:
      "linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)",
    backgroundSize: "200% 100%",
    animation: "panelShimmer 1.6s ease-in-out infinite",
    animationDelay: delay,
  } as const
}

function RightPanelLoadingSkeleton() {
  return (
    <>
      <style>
        {`
          @keyframes panelShimmer {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
          }
        `}
      </style>

      <div className="mb-4 flex items-center gap-2">
        <div className="h-10 w-20 rounded-xl" style={shimmerStyle("0s")} />
        <div className="h-10 w-16 rounded-xl" style={shimmerStyle("0.06s")} />
      </div>

      <div className="mb-4">
        <div className="mb-2 h-4 w-20 rounded" style={shimmerStyle("0.1s")} />
        <div className="h-[52px] w-full rounded-2xl" style={shimmerStyle("0.14s")} />
      </div>

      <div className="mb-4 max-h-[420px] space-y-3 overflow-hidden pr-1">
        {[...Array(4)].map((_, idx) => (
          <div
            key={idx}
            className="w-full rounded-[22px] border border-[#dbe7ff] bg-white p-4"
          >
            <div className="mb-2 flex items-center justify-between gap-3">
              <div
                className="h-6 w-12 rounded-full"
                style={shimmerStyle(`${0.18 + idx * 0.05}s`)}
              />
              <div
                className="h-5 w-24 rounded-full"
                style={shimmerStyle(`${0.22 + idx * 0.05}s`)}
              />
            </div>

            <div
              className="mb-2 h-4 w-[68%] rounded"
              style={shimmerStyle(`${0.26 + idx * 0.05}s`)}
            />
            <div
              className="h-4 w-[42%] rounded"
              style={shimmerStyle(`${0.3 + idx * 0.05}s`)}
            />
          </div>
        ))}
      </div>

      <div
        className="mb-4 h-[52px] w-full rounded-2xl"
        style={shimmerStyle("0.38s")}
      />

      <div className="rounded-2xl border border-[#e5ecff] bg-[#fafcff] px-4 py-3">
        {[...Array(6)].map((_, idx) => (
          <div key={idx} className="mb-2 flex items-center gap-2 last:mb-0">
            <div
              className="h-4 w-20 rounded"
              style={shimmerStyle(`${0.42 + idx * 0.04}s`)}
            />
            <div
              className="h-4 w-16 rounded"
              style={shimmerStyle(`${0.46 + idx * 0.04}s`)}
            />
          </div>
        ))}
      </div>
    </>
  )
}

function fakePageCount(paragraphs: ParagraphItem[], tables: TableItem[]) {
  const pageValues = [
    ...paragraphs.map((p) => p.page),
    ...tables.map((t) => t.page),
  ].filter((v): v is number => typeof v === "number" && !Number.isNaN(v))

  if (pageValues.length === 0) return 1
  return Math.max(...pageValues)
}

function findBoxByParagraphId(boxes: OverlayBox[], paragraphId: string) {
  return boxes.find((box) => box.paragraphId === paragraphId) ?? null
}

function formatSavedAt(value: string) {
  try {
    return new Date(value).toLocaleString("ko-KR", {
      hour12: false,
    })
  } catch {
    return value
  }
}

function getHistoryTypeLabel(item: SavedSelectionHistoryItem) {
  if (item.side === "template") return "템플릿"
  return "원본문서"
}

function getHistoryTypeStyle(item: SavedSelectionHistoryItem) {
  if (item.side === "template") {
    return "bg-violet-50 text-violet-700 border-violet-100"
  }
  return "bg-amber-50 text-amber-700 border-amber-100"
}

function getBlockTypeStyle(item: SavedSelectionHistoryItem) {
  if (item.blockType === "table") {
    return "bg-blue-50 text-blue-700 border-blue-100"
  }
  return "bg-emerald-50 text-emerald-700 border-emerald-100"
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
  const [historySearch, setHistorySearch] = useState("")

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

  const [isApplyResultOpen, setIsApplyResultOpen] = useState(false)
  const [savedSelectionHistory, setSavedSelectionHistory] = useState<
    SavedSelectionHistoryItem[]
  >([])

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

  const currentPageParagraphIdSet = useMemo(
    () => new Set(visibleParagraphs.map((p) => p.paragraphId)),
    [visibleParagraphs]
  )

  const currentPageTableIdSet = useMemo(
    () => new Set(visibleTables.map((t) => t.tableId)),
    [visibleTables]
  )

  const visibleBoxes = useMemo(() => {
    return currentBoxes.filter((box) => {
      if (box.paragraphId) {
        return currentPageParagraphIdSet.has(box.paragraphId)
      }

      const matchedTableId = Array.from(currentPageTableIdSet).find(
        (tableId) => box.id === `${tableId}-box`
      )
      return Boolean(matchedTableId)
    })
  }, [currentBoxes, currentPageParagraphIdSet, currentPageTableIdSet])

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
      labelText:
        t.tableKind === "oneline_box"
          ? `[ONELINE_BOX] ${t.cellTexts?.[0]?.join(" | ") || "(빈 표)"}`
          : t.cellTexts?.[0]?.join(" | ") || "(빈 표)",
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

  const filteredHistory = useMemo(() => {
    const keyword = historySearch.trim().toLowerCase()
    if (!keyword) return savedSelectionHistory

    return savedSelectionHistory.filter((item) => {
      const targetId = item.blockType === "table" ? item.tableId ?? "" : item.paragraphId ?? ""
      return (
        item.side.toLowerCase().includes(keyword) ||
        item.label.toLowerCase().includes(keyword) ||
        item.blockType.toLowerCase().includes(keyword) ||
        targetId.toLowerCase().includes(keyword)
      )
    })
  }, [savedSelectionHistory, historySearch])

  useEffect(() => {
    setCurrentPage(1)
    setSelectedBoxId(null)
  }, [activeSide])

  useEffect(() => {
    setSelectedBoxId(null)

    if (activeSide === "template") {
      setSelectedTemplateParagraphId("")
      setSelectedTemplateTableId("")
    } else {
      setSelectedSourceParagraphId("")
      setSelectedSourceTableId("")
    }
  }, [currentPage, activeSide])

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

  function resetPreviewState() {
    setCurrentPage(1)
    setActiveSide("template")
    setActivePanel("paragraph")

    setTemplateParagraphs([])
    setSourceParagraphs([])
    setTemplateTables([])
    setSourceTables([])

    setSelectedTemplateParagraphId("")
    setSelectedSourceParagraphId("")
    setSelectedTemplateTableId("")
    setSelectedSourceTableId("")

    setTemplateBoxes([])
    setSourceBoxes([])
    setSelectedBoxId(null)

    setApplyResult(null)
    setSavedSelectionHistory([])
    setSavedMessage("")
    setError("")
    setHistorySearch("")
  }

  function handleRemoveSavedHistoryItem(itemId: string) {
    const target = savedSelectionHistory.find((item) => item.id === itemId)
    if (!target) return

    setSavedSelectionHistory((prev) => prev.filter((item) => item.id !== itemId))

    if (target.side === "template") {
      if (target.blockType === "paragraph" && target.paragraphId) {
        setTemplateBoxes((prev) =>
          prev.filter((box) => box.paragraphId !== target.paragraphId)
        )

        if (selectedTemplateParagraphId === target.paragraphId) {
          setSelectedTemplateParagraphId("")
        }
      }

      if (target.blockType === "table" && target.tableId) {
        setTemplateBoxes((prev) =>
          prev.filter((box) => box.id !== `${target.tableId}-box`)
        )

        if (selectedTemplateTableId === target.tableId) {
          setSelectedTemplateTableId("")
        }
      }
    } else {
      if (target.blockType === "paragraph" && target.paragraphId) {
        setSourceBoxes((prev) =>
          prev.filter((box) => box.paragraphId !== target.paragraphId)
        )

        if (selectedSourceParagraphId === target.paragraphId) {
          setSelectedSourceParagraphId("")
        }
      }

      if (target.blockType === "table" && target.tableId) {
        setSourceBoxes((prev) =>
          prev.filter((box) => box.id !== `${target.tableId}-box`)
        )

        if (selectedSourceTableId === target.tableId) {
          setSelectedSourceTableId("")
        }
      }
    }

    setSelectedBoxId(null)
    setSavedMessage(
      `${target.side} · ${target.label} · ${target.blockType} 항목을 목록/박스에서 해제했습니다.`
    )
  }

  async function handlePreviewUpload() {
    if (!templateFile || !sourceFile) {
      alert("템플릿 파일과 source 파일을 모두 선택해주세요.")
      return
    }

    try {
      setUploading(true)
      resetPreviewState()

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

      setCurrentPage(1)
      setActiveSide("template")
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

      const historyItem: SavedSelectionHistoryItem = {
        id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
        sessionId,
        side: data.side,
        page: data.page ?? currentPage,
        label: data.label,
        blockType: data.blockType,
        paragraphId: data.paragraphId ?? undefined,
        tableId: data.tableId ?? undefined,
        savedAt: new Date().toISOString(),
      }

      // setSavedSelectionHistory((prev) => [historyItem, ...prev])
      setSavedSelectionHistory((prev) => {
        if (prev.length >= 20) {
          alert("최근 저장 항목은 최대 20개까지 저장할 수 있습니다.")
          return prev
        }
        return [historyItem, ...prev]
      })

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

      const res = await fetch(`${API_BASE_URL}/api/v1/compose/apply-all`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-API-Key": apiKey,
        },
        body: JSON.stringify({
          sessionId,
          label: currentRole,
          fontPolicy: "original",
        }),
      })

      const raw = await res.text()

      let data: any = null
      try {
        data = raw ? JSON.parse(raw) : null
      } catch {
        data = raw
      }

      if (!res.ok) {
        console.error("apply error response:", data)
        throw new Error(
          typeof data === "string"
            ? data
            : data?.message || data?.error || "apply 실패"
        )
      }

      setApplyResult(data)
      setIsApplyResultOpen(false)

      alert("템플릿 서식으로 변경 완료하였습니다.")
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "템플릿 서식으로 변경 실패했습니다.")
    } finally {
      setApplying(false)
    }
  }

  async function triggerDownload(downloadUrl: string, fileName?: string) {
    const apiKey = localStorage.getItem("apiKey") ?? ""

    const res = await fetch(`${API_BASE_URL}${downloadUrl}`, {
      headers: apiKey ? { "X-API-Key": apiKey } : {},
    })

    if (!res.ok) {
      throw new Error("파일 다운로드에 실패했습니다.")
    }

    const blob = await res.blob()
    const objectUrl = URL.createObjectURL(blob)

    const link = document.createElement("a")
    link.href = objectUrl
    link.download = fileName || "output.hwpx"
    document.body.appendChild(link)
    link.click()
    link.remove()

    URL.revokeObjectURL(objectUrl)
  }

  return (
  <>
    <TopNav onNavigate={onNavigate} currentPage="composeStudio" onLogout={onLogout} />

    <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
      <div className="flex">
        <aside className="sticky top-[64px] hidden h-[calc(100vh-64px)] w-[336px] shrink-0 flex-col border-r border-slate-200/80 bg-white/90 backdrop-blur xl:flex">
          <div className="mt-5 flex items-center justify-between px-5">
            <div className="flex items-center gap-2">
              <Clock3 className="h-4 w-4 text-[#1f69ef]" />
              <h2 className="text-sm font-semibold text-[#162033]">
                최근 저장 항목
              </h2>
            </div>
            <span className="text-xs font-medium text-slate-500">
              {filteredHistory.length}개
            </span>
          </div>

          <div className="px-5 pt-5">
            <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
              <Search className="h-4 w-4 text-slate-400" />
              <input
                value={historySearch}
                onChange={(e) => setHistorySearch(e.target.value)}
                className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                placeholder="저장 항목 검색"
              />
            </div>
          </div>

          <div className="custom-scroll mt-3 min-h-0 flex-1 overflow-y-auto px-4 pb-5">
            <div className="space-y-2">
              {filteredHistory.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-4 py-8 text-center text-sm text-slate-400">
                  아직 저장된 항목이 없습니다.
                </div>
              ) : (
                filteredHistory.map((item) => {
                  const targetId =
                    item.blockType === "table"
                      ? item.tableId ?? "-"
                      : item.paragraphId ?? "-"

                  return (
                    <div
                      key={item.id}
                      className="rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-[#cfe0ff] hover:bg-[#f8fbff] hover:shadow-sm"
                    >
                      <div className="mb-1 text-right">
                        <span className="text-[11px] text-slate-400">
                          {formatSavedAt(item.savedAt)}
                        </span>
                      </div>

                      <div className="text-[18px] font-semibold leading-tight text-[#162033]">
                        {item.label}
                      </div>

                      <div className="mt-[2px] text-[11px] text-slate-400">
                        page {item.page}
                      </div>

                      <div className="mt-[4px] flex items-center gap-1.5">
                        <span
                          className={`inline-flex rounded-full border px-2 py-[3px] text-[10px] font-semibold ${getHistoryTypeStyle(
                            item
                          )}`}
                        >
                          {getHistoryTypeLabel(item)}
                        </span>

                        <span
                          className={`inline-flex rounded-full border px-2 py-[3px] text-[10px] font-semibold ${getBlockTypeStyle(
                            item
                          )}`}
                        >
                          {item.blockType}
                        </span>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemoveSavedHistoryItem(item.id)}
                        className="mt-3 inline-flex w-full items-center justify-center gap-1 rounded-xl bg-[#111827] px-3 py-2 text-xs font-semibold text-white transition hover:bg-black"
                      >
                        <X className="h-3.5 w-3.5 text-white" />
                        <span className="text-white">취소</span>
                      </button>
                    </div>
                  )
                })
              )}
            </div>
          </div>
        </aside>

          {/* 메인 */}
          <main className="flex-1">
            <div className="mx-auto max-w-[1380px] px-4 py-6 sm:px-6 lg:px-8">
              <header className="mb-6 rounded-[32px] border border-[#cfe0ff] bg-white/85 p-6 backdrop-blur">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="flex items-start gap-3">
                    <button className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-600 xl:hidden">
                      <PanelLeft className="h-5 w-5" />
                    </button>

                    <div>
                      <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                        <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
                        Compose Studio
                      </div>
                      <h1 className="text-3xl font-bold tracking-tight text-[#162033] sm:text-4xl">
                        템플릿 서식 자동 적용
                      </h1>
                      <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                        10초만에 원하는 스타일의 문서로 자동 제작
                      </p>
                    </div>
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

                  <div className="mb-3 pl-6 text-[11px] text-[#7a869f]">
                    기존 한글 문서와 완전히 동일하지 않을 수 있습니다.
                  </div>

                  <ComposePageParagraphLayer
                    paragraphs={visibleParagraphs}
                    tables={visibleTables}
                    selectedParagraphId={currentSelectedId || null}
                    selectedTableId={currentSelectedTableId || null}
                    boxes={visibleBoxes}
                    selectedBoxId={selectedBoxId}
                    isLoading={uploading}
                    onSelectParagraph={(paragraphId) => {
                      const existingBox = currentBoxes.find((box) => box.paragraphId === paragraphId)

                      if (activeSide === "template") {
                        setSelectedTemplateParagraphId(paragraphId)
                        setSelectedTemplateTableId("")
                      } else {
                        setSelectedSourceParagraphId(paragraphId)
                        setSelectedSourceTableId("")
                      }

                      if (existingBox) {
                        setSelectedBoxId(existingBox.id)
                        return
                      }

                      const paragraph = visibleParagraphs.find((p) => p.paragraphId === paragraphId)
                      if (!paragraph) return

                      const autoBox: OverlayBox = {
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
                    onSelectTable={(tableId) => {
                      if (activeSide === "template") {
                        setSelectedTemplateTableId(tableId)
                        setSelectedTemplateParagraphId("")
                      } else {
                        setSelectedSourceTableId(tableId)
                        setSelectedSourceParagraphId("")
                      }

                      const table = visibleTables.find((t) => t.tableId === tableId)
                      if (!table) return

                      const tableBox: OverlayBox = {
                        id: `${table.tableId}-box`,
                        x: table.x ?? 56,
                        y: table.y ?? 80,
                        width: table.width ?? 420,
                        height: table.height ?? 120,
                        label: currentRole,
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
                    onSelectBox={(boxId) => {
                      setSelectedBoxId(boxId)

                      const clickedBox = currentBoxes.find((box) => box.id === boxId)
                      if (!clickedBox) return

                      if (clickedBox.paragraphId) {
                        if (activeSide === "template") {
                          setSelectedTemplateParagraphId(clickedBox.paragraphId)
                          setSelectedTemplateTableId("")
                        } else {
                          setSelectedSourceParagraphId(clickedBox.paragraphId)
                          setSelectedSourceTableId("")
                        }
                        return
                      }

                      const matchedTableId = Array.from(currentPageTableIdSet).find(
                        (tableId) => boxId === `${tableId}-box`
                      )

                      if (matchedTableId) {
                        if (activeSide === "template") {
                          setSelectedTemplateTableId(matchedTableId)
                          setSelectedTemplateParagraphId("")
                        } else {
                          setSelectedSourceTableId(matchedTableId)
                          setSelectedSourceParagraphId("")
                        }
                      }
                    }}
                  />
                </div>

                <div className="rounded-[32px] border border-[#dbe7ff] bg-white p-6">
                  <div className="mb-4 flex items-center gap-2 text-lg font-semibold text-[#162033]">
                    <Tag className="h-5 w-5 text-[#1f69ef]" />
                    선택 패널
                  </div>

                  {uploading ? (
                    <RightPanelLoadingSkeleton />
                  ) : (
                    <>
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

                      <div className="custom-scroll mb-4 max-h-[420px] space-y-3 overflow-y-auto pr-1">
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
                                      setSelectedTemplateTableId("")
                                    } else {
                                      setSelectedSourceParagraphId(p.paragraphId)
                                      setSelectedSourceTableId("")
                                    }

                                    if (existingBox) {
                                      setSelectedBoxId(existingBox.id)
                                      return
                                    }

                                    const autoBox: OverlayBox = {
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
                                    setSelectedTemplateParagraphId("")
                                  } else {
                                    setSelectedSourceTableId(t.tableId)
                                    setSelectedSourceParagraphId("")
                                  }

                                  const tableBox: OverlayBox = {
                                    id: `${t.tableId}-box`,
                                    x: t.x ?? 56,
                                    y: t.y ?? 80,
                                    width: t.width ?? 420,
                                    height: t.height ?? 120,
                                    label: currentRole,
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
                    </>
                  )}
                </div>
              </section>

              {applyResult ? (
                <section className="mt-6 rounded-[32px] border border-[#dbe7ff] bg-white p-6">
                  <details
                    open={isApplyResultOpen}
                    onToggle={(e) => setIsApplyResultOpen((e.target as HTMLDetailsElement).open)}
                    className="group"
                  >
                    <summary className="cursor-pointer list-none text-xl font-semibold text-[#162033]">
                      <div className="flex items-center justify-between gap-3">
                        <span>apply 결과</span>
                        <span className="text-sm font-medium text-[#63708a] group-open:hidden">
                          펼쳐보기
                        </span>
                        <span className="hidden text-sm font-medium text-[#63708a] group-open:inline">
                          접기
                        </span>
                      </div>
                    </summary>

                    <pre className="mt-4 overflow-x-auto rounded-2xl bg-[#0f172a] p-4 text-xs text-slate-100">
                      {JSON.stringify(applyResult, null, 2)}
                    </pre>

                    {applyResult?.downloadUrl ? (
                      <div className="mt-4">
                        <button
                          onClick={() =>
                            triggerDownload(
                              applyResult.downloadUrl,
                              applyResult.fileId || "output.hwpx"
                            )
                          }
                          className="rounded-xl bg-[#1f69ef] px-4 py-2 text-sm font-semibold text-white hover:bg-[#1859ca]"
                        >
                          결과 파일 다운로드
                        </button>
                      </div>
                    ) : null}
                  </details>
                </section>
              ) : null}
            </div>
          </main>
        </div>
      </div>
    </>
  )
}