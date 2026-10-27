import type { PageType } from "../App"
import type { OverlayBox } from "../components/ComposePageOverlay"
import TopNav from "../components/TopNav"
import ComposePageParagraphLayer from "../components/ComposePageParagraphLayer"
import { useEffect, useMemo, useRef, useState } from "react"
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
  RefreshCcw,
  Home,
  Settings,
  FileText,
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

type VisualEndpoint = {
  id: string
  kind: "paragraph" | "table"
  page: number
  text: string
}

type VisualMapping = {
  label: string
  template: VisualEndpoint
  source: VisualEndpoint
}

type ConnectorLine = { label: string; x1: number; y1: number; x2: number; y2: number }

type SavedSelectionHistoryItem = {
  id: string
  sessionId: string
  side: "template" | "source"
  page: number
  label: string
  blockType: "paragraph" | "table"
  paragraphId?: string
  tableId?: string
  // 추가: 좌측 최근 저장 카드에 태깅된 실제 문서 내용을 보여주기 위한 필드
  displayText?: string
  savedAt: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "https://api.autohwpx.com"

const GUIDE_VIDEO_URL =
  import.meta.env.VITE_STUDIO_GUIDE_VIDEO_URL ?? "/videos/studio-guide.mp4"

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
        <div className="h-10 w-20 rounded-none" style={shimmerStyle("0s")} />
        <div className="h-10 w-16 rounded-none" style={shimmerStyle("0.06s")} />
      </div>

      <div className="mb-4">
        <div className="mb-2 h-4 w-20 rounded" style={shimmerStyle("0.1s")} />
        <div className="h-[52px] w-full rounded-none" style={shimmerStyle("0.14s")} />
      </div>

      <div className="mb-4 max-h-[420px] space-y-3 overflow-hidden pr-1">
        {[...Array(4)].map((_, idx) => (
          <div
            key={idx}
            className="w-full rounded-sm border border-[#e0e0e0] bg-white p-4"
          >
            <div className="mb-2 flex items-center justify-between gap-3">
              <div
                className="h-6 w-12 rounded-sm"
                style={shimmerStyle(`${0.18 + idx * 0.05}s`)}
              />
              <div
                className="h-5 w-24 rounded-sm"
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
        className="mb-4 h-[52px] w-full rounded-none"
        style={shimmerStyle("0.38s")}
      />

      <div className="rounded-none border border-[#e5ecff] bg-[#f4f4f4] px-4 py-3">
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

  const [highlightSourceTab, setHighlightSourceTab] = useState(false)
  // Visual mapping: click a template block, then click a source block.
  const [templateMappingPage, setTemplateMappingPage] = useState(1)
  const [sourceMappingPage, setSourceMappingPage] = useState(1)
  const [pendingTemplate, setPendingTemplate] = useState<VisualEndpoint | null>(null)
  const [visualMappings, setVisualMappings] = useState<VisualMapping[]>([])
  const [mappingBusy, setMappingBusy] = useState(false)
  const [mappingError, setMappingError] = useState("")
  const [connectorLines, setConnectorLines] = useState<ConnectorLine[]>([])
  const [pendingConnector, setPendingConnector] = useState<ConnectorLine | null>(null)
  const mappingContainerRef = useRef<HTMLDivElement | null>(null)
  const templateMappingRef = useRef<HTMLDivElement | null>(null)
  const sourceMappingRef = useRef<HTMLDivElement | null>(null)


  const [creditToast, setCreditToast] = useState<{
    show: boolean
    message: string
    subMessage?: string
  }>({
    show: false,
    message: "",
  })

  const templateMappingCount = useMemo(() => fakePageCount(templateParagraphs, templateTables), [templateParagraphs, templateTables])
  const sourceMappingCount = useMemo(() => fakePageCount(sourceParagraphs, sourceTables), [sourceParagraphs, sourceTables])
  const visibleTemplateMappingParagraphs = useMemo(() => templateParagraphs.filter(p => p.page == null || p.page === templateMappingPage), [templateParagraphs, templateMappingPage])
  const visibleTemplateMappingTables = useMemo(() => templateTables.filter(t => t.page == null || t.page === templateMappingPage), [templateTables, templateMappingPage])
  const visibleSourceMappingParagraphs = useMemo(() => sourceParagraphs.filter(p => p.page == null || p.page === sourceMappingPage), [sourceParagraphs, sourceMappingPage])
  const visibleSourceMappingTables = useMemo(() => sourceTables.filter(t => t.page == null || t.page === sourceMappingPage), [sourceTables, sourceMappingPage])

  function getMappingEndpoint(side: "template" | "source", kind: "paragraph" | "table", id: string): VisualEndpoint | null {
    const page = side === "template" ? templateMappingPage : sourceMappingPage
    if (kind === "paragraph") {
      const item = (side === "template" ? templateParagraphs : sourceParagraphs).find(p => p.paragraphId === id)
      return item ? { id, kind, page, text: item.text || "(빈 문단)" } : null
    }
    const item = (side === "template" ? templateTables : sourceTables).find(t => t.tableId === id)
    return item ? { id, kind, page, text: item.cellTexts?.[0]?.join(" | ") || "(빈 표)" } : null
  }

  async function postVisualSelection(side: "template" | "source", endpoint: VisualEndpoint, label: string) {
    const res = await fetch(`${API_BASE_URL}/api/v1/compose/selections`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-API-Key": localStorage.getItem("apiKey") ?? "" },
      body: JSON.stringify({
        sessionId, side, page: endpoint.page, label, blockType: endpoint.kind,
        ...(endpoint.kind === "paragraph" ? { paragraphId: endpoint.id } : { tableId: endpoint.id }),
      }),
    })
    if (!res.ok) throw new Error(`${side === "template" ? "템플릿" : "원본"} 매핑 저장 실패 (${res.status})`)
  }

  // The current backend /apply-all recognizes fixed role labels only.
  // Reuse a label when the same template block is connected to more source blocks.
  function resolveMappingLabel(template: VisualEndpoint): string | null {
    const previous = visualMappings.find(m => m.template.kind === template.kind && m.template.id === template.id)
    if (previous) return previous.label
    const used = new Set(savedSelectionHistory.map(item => item.label))
    if (template.kind === "table") {
      const table = templateTables.find(t => t.tableId === template.id)
      const preferred = table?.tableKind === "oneline_box" ? "oneline_box" : "table"
      return used.has(preferred) ? null : preferred
    }
    return ["heading1", "heading2", "heading3", "body", "caption"].find(label => !used.has(label)) ?? null
  }

  async function handleMappingClick(side: "template" | "source", kind: "paragraph" | "table", id: string) {
    if (!sessionId || mappingBusy) return
    const endpoint = getMappingEndpoint(side, kind, id)
    if (!endpoint) return
    setMappingError("")
    if (side === "template") { setPendingTemplate(endpoint); return }
    if (!pendingTemplate) { setMappingError("왼쪽 템플릿에서 서식을 먼저 선택해주세요."); return }
    if (pendingTemplate.kind !== endpoint.kind) {
      setMappingError("문단은 문단끼리, 표는 표끼리 연결해주세요.")
      return
    }
    if (visualMappings.some(m => m.source.kind === endpoint.kind && m.source.id === endpoint.id)) {
      setMappingError("이미 연결된 원본문서 영역입니다.")
      return
    }
    const label = resolveMappingLabel(pendingTemplate)
    if (!label) {
      setMappingError("현재 백엔드의 서식 역할 슬롯을 모두 사용했어요. 같은 템플릿 영역을 여러 원본 영역에 연결할 수 있지만, 새로운 서식 종류를 더 추가하려면 백엔드 변경이 필요합니다.")
      return
    }
    const existingTemplate = visualMappings.some(m => m.label === label)
    setMappingBusy(true)
    try {
      if (!existingTemplate) await postVisualSelection("template", pendingTemplate, label)
      await postVisualSelection("source", endpoint, label)
      setVisualMappings(prev => [...prev, { label, template: pendingTemplate, source: endpoint }])
      const savedAt = new Date().toISOString()
      setSavedSelectionHistory(prev => [
        ...(!existingTemplate ? [{ id: `${label}_template`, sessionId, side: "template" as const, page: pendingTemplate.page, label,
          blockType: pendingTemplate.kind, ...(pendingTemplate.kind === "paragraph" ? { paragraphId: pendingTemplate.id } : { tableId: pendingTemplate.id }),
          displayText: pendingTemplate.text, savedAt }] : []),
        { id: `${label}_${endpoint.id}_source`, sessionId, side: "source" as const, page: endpoint.page, label,
          blockType: endpoint.kind, ...(endpoint.kind === "paragraph" ? { paragraphId: endpoint.id } : { tableId: endpoint.id }),
          displayText: endpoint.text, savedAt },
        ...prev,
      ])
      setPendingTemplate(null)
      setSavedMessage(`${label} 서식 연결이 자동 저장되었습니다.`)
    } catch (err) {
      // This backend does not provide a selection DELETE endpoint. If one POST succeeded,
      // require a fresh preview session rather than pretending to roll it back.
      setMappingError(`${err instanceof Error ? err.message : "매핑 저장 실패"}. 일부 선택이 서버에 저장되었을 수 있습니다. 파일 불러오기를 다시 눌러 새 세션으로 시작해주세요.`)
    } finally { setMappingBusy(false) }
  }

  // Connect the real preview rectangles, not hardcoded pixel coordinates.
  function updateConnectorLines() {
    const container = mappingContainerRef.current
    if (!container) return
    const bounds = container.getBoundingClientRect()
    const findBlock = (root: HTMLDivElement | null, ep: VisualEndpoint) => {
      if (!root) return null
      return Array.from(root.querySelectorAll<HTMLElement>("[data-compose-id]")).find(
        el => el.dataset.composeId === ep.id && el.dataset.composeKind === ep.kind
      ) ?? null
    }
    const lines: ConnectorLine[] = []
    for (const mapping of visualMappings) {
      if (mapping.template.page !== templateMappingPage || mapping.source.page !== sourceMappingPage) continue
      const a = findBlock(templateMappingRef.current, mapping.template)
      const b = findBlock(sourceMappingRef.current, mapping.source)
      if (!a || !b) continue
      const ar = a.getBoundingClientRect(), br = b.getBoundingClientRect()
      lines.push({ label: mapping.label, x1: ar.right - bounds.left, y1: ar.top + ar.height/2 - bounds.top,
        x2: br.left - bounds.left, y2: br.top + br.height/2 - bounds.top })
    }
    setConnectorLines(lines)
    if (pendingTemplate && pendingTemplate.page === templateMappingPage) {
      const selected = findBlock(templateMappingRef.current, pendingTemplate)
      const target = sourceMappingRef.current
      if (selected && target) {
        const a = selected.getBoundingClientRect(), b = target.getBoundingClientRect()
        setPendingConnector({ label: "pending", x1: a.right - bounds.left, y1: a.top + a.height / 2 - bounds.top,
          x2: b.left - bounds.left, y2: a.top + a.height / 2 - bounds.top })
      } else { setPendingConnector(null) }
    } else { setPendingConnector(null) }
  }

  useEffect(() => {
    updateConnectorLines()
    const listener = () => updateConnectorLines()
    window.addEventListener("resize", listener)
    const observer = new ResizeObserver(listener)
    if (mappingContainerRef.current) observer.observe(mappingContainerRef.current)
    return () => { window.removeEventListener("resize", listener); observer.disconnect() }
  }, [visualMappings, pendingTemplate, templateMappingPage, sourceMappingPage, sessionId])

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
        targetId.toLowerCase().includes(keyword) ||
        (item.displayText ?? "").toLowerCase().includes(keyword)
      )
    })
  }, [savedSelectionHistory, historySearch])

  const [showGuideModal, setShowGuideModal] = useState(false)

  {/*
  useEffect(() => {
    const seen = sessionStorage.getItem("studioGuideSeen")

    if (!seen) {
      setShowGuideModal(true)
      sessionStorage.setItem("studioGuideSeen", "true")
    }
  }, [])
  */}

  useEffect(() => {
    setShowGuideModal(true)
  }, [])

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

  // 파일 선택은 미리보기 로딩과 별개입니다. 서버 반영은 "파일 불러오기" 클릭 시 시작합니다.
  function handleSelectDocumentFile(kind: "template" | "source", file: File) {
    if (!file.name.toLowerCase().endsWith(".hwpx")) {
      alert(".hwpx 확장자 파일만 업로드할 수 있습니다.")
      return
    }

    if (kind === "template") {
      setTemplateFile(file)
    } else {
      setSourceFile(file)
    }
    setSessionId("")
    resetPreviewState()
    alert("파일 불러오기 버튼을 클릭해주세요.")
  }

  async function loadSampleFile(url: string, filename: string, type: string) {
    const res = await fetch(url)
    if (!res.ok) {
      throw new Error(`샘플 파일을 불러오지 못했습니다: ${filename}`)
    }

    const blob = await res.blob()
    return new File([blob], filename, { type })
  }

  async function handleLoadSample(kind: "template" | "source") {
    try {
      if (kind === "template") {
        const file = await loadSampleFile(
          "/samples/sample-template.hwpx",
          "sample-template.hwpx",
          "application/octet-stream"
        )
        handleSelectDocumentFile("template", file)
      } else {
        const file = await loadSampleFile(
          "/samples/sample-source.hwpx",
          "sample-source.hwpx",
          "application/octet-stream"
        )
        handleSelectDocumentFile("source", file)
      }
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "샘플 파일 불러오기 실패")
    }
  }

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
    setPendingTemplate(null)
    setVisualMappings([])
    setMappingError("")
    setTemplateMappingPage(1)
    setSourceMappingPage(1)
  }

  function handleRemoveSavedHistoryItem(itemId: string) {
    const target = savedSelectionHistory.find((item) => item.id === itemId)
    if (!target) return
    if (visualMappings.some(m => m.label === target.label)) {
      alert("시각적 연결은 서버에 저장되어 있어 개별 삭제할 수 없습니다. 연결을 다시 지정하려면 '파일 불러오기'를 눌러 새 세션을 시작해주세요.")
      return
    }

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

  function isSavedBox(side: "template" | "source", box: OverlayBox) {
    return savedSelectionHistory.some((item) => {
      if (item.side !== side) return false

      if (box.paragraphId) {
        return item.blockType === "paragraph" && item.paragraphId === box.paragraphId
      }

      if (box.id.endsWith("-box")) {
        const tableId = box.id.replace(/-box$/, "")
        return item.blockType === "table" && item.tableId === tableId
      }

      return false
    })
  }

  function replaceDraftBoxForActiveSide(nextBox: OverlayBox) {
    if (activeSide === "template") {
      setTemplateBoxes((prev) => [
        ...prev.filter((box) => isSavedBox("template", box)),
        nextBox,
      ])
    } else {
      setSourceBoxes((prev) => [
        ...prev.filter((box) => isSavedBox("source", box)),
        nextBox,
      ])
    }

    setSelectedBoxId(nextBox.id)
  }

  function handleClearPreviewBoxes() {
    if (visualMappings.length > 0) {
      alert("연결된 서식은 서버에 저장되어 있습니다. 연결을 초기화하려면 '파일 불러오기'를 다시 눌러 새 세션을 시작해주세요.")
      return
    }
    setTemplateBoxes([])
    setSourceBoxes([])
    setSelectedBoxId(null)

    setSelectedTemplateParagraphId("")
    setSelectedSourceParagraphId("")
    setSelectedTemplateTableId("")
    setSelectedSourceTableId("")

    setSavedSelectionHistory([])
    setSavedMessage("")
    setApplyResult(null)
    setHistorySearch("")
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
        credentials: "include",
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
      alert("파일을 정상적으로 불러왔습니다.")
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
        credentials: "include",
        body: JSON.stringify(body),
      })

      if (!res.ok) {
        throw new Error("선택 저장에 실패했습니다.")
      }

      const data = await res.json()
      const savedTarget = data.blockType === "table" ? data.tableId : data.paragraphId

      // 추가:
      // 좌측 최근 저장 카드에서 label 옆에 보여줄 실제 문서 내용
      const selectedDisplayText =
        activePanel === "table"
          ? (
              activeSide === "template"
                ? templateTables.find((t) => t.tableId === tableId)
                : sourceTables.find((t) => t.tableId === tableId)
            )?.cellTexts?.[0]?.join(" | ") ?? ""
          : (
              activeSide === "template"
                ? templateParagraphs.find((p) => p.paragraphId === paragraphId)
                : sourceParagraphs.find((p) => p.paragraphId === paragraphId)
            )?.text ?? ""

      const historyItem: SavedSelectionHistoryItem = {
        id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
        sessionId,
        side: data.side,
        page: data.page ?? currentPage,
        label: data.label,
        blockType: data.blockType,
        paragraphId: data.paragraphId ?? undefined,
        tableId: data.tableId ?? undefined,
        displayText: selectedDisplayText,
        savedAt: new Date().toISOString(),
      }

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

  function triggerSourceTabHighlight() {
    setHighlightSourceTab(true)

    setTimeout(() => {
      setHighlightSourceTab(false)
    }, 1800)
  }

  async function handleApply() {
    if (!sessionId) {
      alert("먼저 preview 업로드를 해주세요.")
      return
    }

    const templateItems = savedSelectionHistory.filter(
      (item) => item.side === "template"
    )

    const sourceItems = savedSelectionHistory.filter(
      (item) => item.side === "source"
    )

    if (templateItems.length === 0 && sourceItems.length === 0) {
      alert("템플릿문서와 원본문서에 태그를 먼저 설정해주세요.")
      return
    }

    if (templateItems.length > 0 && sourceItems.length === 0) {
      alert("원본문서에 태그가 설정되지 않았습니다.")
      setActiveSide("source")
      triggerSourceTabHighlight()
      return
    }

    if (templateItems.length === 0 && sourceItems.length > 0) {
      alert("템플릿문서에 태그가 설정되지 않았습니다.")
      return
    }

    const templateLabels = new Set(templateItems.map((item) => item.label))
    const sourceLabels = new Set(sourceItems.map((item) => item.label))

    const isSameSize = templateLabels.size === sourceLabels.size
    const isAllMatched = [...templateLabels].every((label) =>
      sourceLabels.has(label)
    )

    if (!isSameSize || !isAllMatched) {
      alert("템플릿과 원본문서의 태그(label)가 서로 일치하지 않습니다.")
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
        credentials: "include",
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
        const message =
          typeof data === "string"
            ? data
            : data?.message || data?.error || "apply 실패"

        if (message.includes("하루 최대 20회까지 실행할 수 있습니다")) {
          throw new Error("하루 최대 20회까지 실행할 수 있습니다.")
        }

        throw new Error(message)
      }

      setApplyResult(data)
      setIsApplyResultOpen(false)

      setCreditToast({
        show: true,
        message: "2코인이 차감되었습니다.",
        subMessage: "템플릿 서식 적용이 완료되었습니다.",
      })

      setTimeout(() => {
        setCreditToast({ show: false, message: "" })
      }, 3500)

      alert("템플릿 서식으로 변경 완료하였습니다.")
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "템플릿 서식으로 변경 실패했습니다.")
    } finally {
      setApplying(false)
    }
  }

  async function triggerDownload(downloadUrl: string, fileName?: string) {
    try {
      const fullUrl = `${API_BASE_URL}${downloadUrl}`
      console.log("download fullUrl =", fullUrl)

      const res = await fetch(fullUrl, {
        method: "GET",
      })

      if (!res.ok) {
        const errorText = await res.text()
        console.error("download error:", errorText)
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
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "파일 다운로드에 실패했습니다.")
    }
  }

  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="composeStudio" onLogout={onLogout} isLoggedIn={true}/>

      {!showGuideModal && (
        <button
          type="button"
          onClick={() => setShowGuideModal(true)}
          className={`fixed right-6 z-[9998] flex items-center gap-2 rounded-sm border border-[#c6c6c6] bg-white px-4 py-3 text-sm font-semibold text-[#0f62fe] shadow-[0_10px_28px_rgba(31,105,239,0.18)] transition hover:-translate-y-0.5 hover:bg-[#f4f4f4] ${
            creditToast.show ? "bottom-28" : "bottom-6"
          }`}
        >
          <span className="text-base">?</span>
          메뉴얼 다시 보기
        </button>
      )}

      {showGuideModal && (
        <div className="fixed inset-0 z-[99999] flex items-center justify-center bg-black/55 px-4 "
          onClick={() => setShowGuideModal(false)}>

          <div className="relative w-full max-w-[1040px] overflow-hidden rounded-none bg-white shadow-xl"
          onClick={(e) => e.stopPropagation()}>
            
            <video
              src={GUIDE_VIDEO_URL}
              autoPlay
              muted
              loop
              playsInline
              preload="metadata"
              className="block w-full bg-black"
              style={{
                filter: "brightness(1) contrast(1) saturate(1)",
              }}
            />

            <div className="flex items-center justify-between gap-3 px-5 py-4">
              <div>
                <div className="text-sm font-semibold text-[#161616]">
                  스튜디오 사용 방법
                </div>
                <div className="mt-1 text-xs text-slate-500">
                  샘플 파일 불러오기부터 다운로드까지 빠르게 확인해보세요.
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowGuideModal(false)}
                className="rounded-none bg-[#0f62fe] px-4 py-2 text-sm font-semibold text-white hover:bg-[#0353e9]"
              >
                바로 시작하기
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="min-h-screen bg-[#f4f4f4] text-[#161616]">
        <div className="flex">
          <aside className="sticky top-[64px] hidden h-[calc(100vh-64px)] w-[188px] shrink-0 flex-col bg-[#0f1419] text-white xl:flex">
            
            <nav className="flex-1 py-4">
              <button type="button" className="flex w-full items-center gap-3 border-l-4 border-[#0f62fe] bg-[#0f62fe]/25 px-5 py-3 text-sm font-semibold text-white">
                <FileText className="h-4 w-4" />
                스튜디오
              </button>
              <button type="button" onClick={() => onNavigate("landing")} className="flex w-full items-center gap-3 border-l-4 border-transparent px-5 py-3 text-sm font-medium text-white/75 transition hover:bg-white/10 hover:text-white">
                <Home className="h-4 w-4" />
                홈
              </button>
            </nav>

            <div className="border-t border-white/10 py-4">
              <button type="button" className="flex w-full items-center gap-3 px-5 py-3 text-sm font-medium text-white/75 transition hover:bg-white/10 hover:text-white">
                <Settings className="h-4 w-4" />
                설정
              </button>
            </div>
          </aside>

          <aside className="sticky top-[64px] hidden h-[calc(100vh-64px)] w-[280px] shrink-0 flex-col border-r border-[#e0e0e0] bg-white xl:flex">
            <div className="mt-5 flex items-center justify-between px-5">
              <div className="flex items-center gap-2">
                <Clock3 className="h-4 w-4 text-[#0f62fe]" />
                <h2 className="text-sm font-semibold text-[#161616]">
                  최근 저장 항목
                </h2>
              </div>
              <span className="text-xs font-medium text-slate-500">
                {filteredHistory.length}개
              </span>
            </div>

            <div className="px-5 pt-5">
              <div className="flex items-center gap-2 rounded-none border border-[#e0e0e0] bg-slate-50 px-3 py-2.5">
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
                  <div className="rounded-none border border-dashed border-[#e0e0e0] bg-white px-4 py-8 text-center text-sm text-slate-400">
                    아직 저장된 항목이 없습니다.
                  </div>
                ) : (
                  filteredHistory.map((item) => (
                    <div
                      key={item.id}
                      className="rounded-none border border-[#e0e0e0] bg-white p-4 transition hover:border-[#e0e0e0] hover:bg-[#f4f4f4] hover:shadow-none"
                    >
                      <div className="mb-1 text-right">
                        <span className="text-[11px] text-slate-400">
                          {formatSavedAt(item.savedAt)}
                        </span>
                      </div>

                      <div className="flex min-w-0 items-start gap-2">
                        <div className="shrink-0 text-[18px] font-semibold leading-tight text-[#161616]">
                          {item.label}
                        </div>

                        {item.displayText ? (
                          <div
                            className="min-w-0 flex-1 truncate pt-[3px] text-[12px] font-medium text-slate-500"
                            title={item.displayText}
                          >
                            {item.displayText}
                          </div>
                        ) : null}
                      </div>

                      <div className="mt-[2px] text-[11px] text-slate-400">
                        page {item.page}
                      </div>

                      <div className="mt-[4px] flex items-center gap-1.5">
                        <span
                          className={`inline-flex rounded-sm border px-2 py-[3px] text-[10px] font-semibold ${getHistoryTypeStyle(
                            item
                          )}`}
                        >
                          {getHistoryTypeLabel(item)}
                        </span>

                        <span
                          className={`inline-flex rounded-sm border px-2 py-[3px] text-[10px] font-semibold ${getBlockTypeStyle(
                            item
                          )}`}
                        >
                          {item.blockType}
                        </span>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemoveSavedHistoryItem(item.id)}
                        className="mt-3 inline-flex w-full items-center justify-center gap-1 rounded-none bg-[#111827] px-3 py-2 text-xs font-semibold text-white transition hover:bg-black"
                      >
                        <X className="h-3.5 w-3.5 text-white" />
                        <span className="text-white">취소</span>
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </aside>

          <main className="flex-1">
            <div className="mx-auto max-w-[1360px] px-6 py-8">
              <header className="mb-6">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="flex items-start gap-3">
                    <button className="inline-flex h-11 w-11 items-center justify-center rounded-none border border-[#e0e0e0] bg-white text-slate-600 xl:hidden">
                      <PanelLeft className="h-5 w-5" />
                    </button>

                    <div>
                      <h1 className="text-3xl font-bold tracking-tight text-[#161616] sm:text-4xl">
                        템플릿 서식 자동 적용
                      </h1>
                      <p className="mt-2 text-sm text-[#525252] sm:text-base">
                        템플릿 문서의 문단/표 서식을 원본 문서에 적용합니다.
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-3">
                    <button
                      onClick={handlePreviewUpload}
                      disabled={uploading}
                      className="inline-flex items-center gap-2 rounded-none bg-[#0f62fe] px-5 py-3 text-sm font-semibold text-white shadow-none hover:bg-[#0353e9] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <FileUp className="h-4 w-4" />
                      {uploading ? "불러오는 중..." : "파일 불러오기"}
                    </button>

                    <button
                      onClick={handleApply}
                      disabled={!sessionId || applying}
                      className="inline-flex items-center gap-2 rounded-none border border-[#c6c6c6] bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-[#f4f4f4] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Sparkles className="h-4 w-4" />
                      {applying ? "적용 중..." : "실행하기"}
                    </button>
                  </div>
                </div>
              </header>

              {error ? (
                <div className="mb-6 rounded-none border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600">
                  {error}
                </div>
              ) : null}

              <section className="mb-6 grid gap-4 lg:grid-cols-2">
                {/* 템플릿 */}
                <div className={`border border-[#e0e0e0] bg-white p-6 ${templateFile && !sessionId ? "autohwpx-upload-pending" : ""}`}>
                  <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-[#0f62fe]">
                    <Layers3 className="h-4 w-4" />
                    템플릿 파일
                  </div>

                  {/* 사용자 문서 업로드: 기존 preview API와 파일 상태를 그대로 사용 */}
                  <label className={`flex h-[56px] items-center justify-center gap-2 rounded-none border border-dashed border-[#c6c6c6] bg-[#f4f4f4] text-sm font-medium text-[#525252] ${uploading ? "cursor-not-allowed opacity-60" : "cursor-pointer hover:border-[#0f62fe] hover:bg-[#edf5ff] hover:text-[#0f62fe]"}`}>
                    <FileUp className="h-4 w-4" />
                    파일 업로드 <span className="text-xs text-[#6f6f6f]">(.hwpx 확장자 첨부가능)</span>
                    <input
                      type="file"
                      accept=".hwpx"
                      className="sr-only"
                      disabled={uploading}
                      onChange={(e) => {
                        const file = e.currentTarget.files?.[0]
                        if (file) handleSelectDocumentFile("template", file)
                        e.currentTarget.value = ""
                      }}
                    />
                  </label>

                  <div className="mt-2 flex items-center justify-between gap-3">
                    <p className="text-sm text-[#525252]">
                      {templateFile ? templateFile.name : "선택된 파일 없음"}
                    </p>

                    {/* ✅ 기존 스타일 유지 */}
                    <button
                      type="button"
                      onClick={() => handleLoadSample("template")}
                      className="shrink-0 text-xs font-medium text-slate-400 hover:text-slate-600"
                    >
                      샘플 파일 불러오기
                    </button>
                  </div>
                </div>

                {/* 원본문서 */}
                <div className={`border border-[#e0e0e0] bg-white p-6 ${sourceFile && !sessionId ? "autohwpx-upload-pending" : ""}`}>
                  <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-[#0f62fe]">
                    <Layers3 className="h-4 w-4" />
                    원본문서 파일
                  </div>

                  {/* 사용자 문서 업로드: 기존 preview API와 파일 상태를 그대로 사용 */}
                  <label className={`flex h-[56px] items-center justify-center gap-2 rounded-none border border-dashed border-[#c6c6c6] bg-[#f4f4f4] text-sm font-medium text-[#525252] ${uploading ? "cursor-not-allowed opacity-60" : "cursor-pointer hover:border-[#0f62fe] hover:bg-[#edf5ff] hover:text-[#0f62fe]"}`}>
                    <FileUp className="h-4 w-4" />
                    파일 업로드 <span className="text-xs text-[#6f6f6f]">(.hwpx 확장자 첨부가능)</span>
                    <input
                      type="file"
                      accept=".hwpx"
                      className="sr-only"
                      disabled={uploading}
                      onChange={(e) => {
                        const file = e.currentTarget.files?.[0]
                        if (file) handleSelectDocumentFile("source", file)
                        e.currentTarget.value = ""
                      }}
                    />
                  </label>

                  <div className="mt-2 flex items-center justify-between gap-3">
                    <p className="text-sm text-[#525252]">
                      {sourceFile ? sourceFile.name : "선택된 파일 없음"}
                    </p>

                    {/* ✅ 기존 스타일 유지 */}
                    <button
                      type="button"
                      onClick={() => handleLoadSample("source")}
                      className="shrink-0 text-xs font-medium text-slate-400 hover:text-slate-600"
                    >
                      샘플 파일 불러오기
                    </button>
                  </div>
                </div>
              </section>

              {/* Visual style mapping: two documents visible at once. */}
              <section className="mb-6 border border-[#e0e0e0] bg-white p-5">
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 className="text-xl font-bold text-[#161616]">서식 연결하기</h2>
                    <p className="mt-1 text-sm text-[#525252]">① 왼쪽 템플릿에서 서식 선택 → ② 오른쪽 원본문서에서 적용할 영역 선택. 연결과 선택 저장이 자동으로 완료됩니다.</p>
                    <p className="mt-1 text-xs text-[#6f6f6f]">같은 템플릿 서식을 여러 원본 영역에 연결할 수 있습니다. 연결을 초기화하려면 파일 불러오기를 다시 눌러주세요.</p>
                  </div>
                  <div className="text-sm font-semibold text-[#0f62fe]">연결 {visualMappings.length}개</div>
                </div>
                {pendingTemplate ? (
                  <div className="mb-3 flex items-center justify-between border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">
                    <span>템플릿 선택 완료: {pendingTemplate.text.slice(0, 45)} — 이제 오른쪽 원본문서에서 연결할 부분을 클릭하세요.</span>
                    <button type="button" onClick={() => setPendingTemplate(null)} className="font-semibold underline">선택 취소</button>
                  </div>
                ) : null}
                {mappingError ? <p role="alert" className="mb-3 text-sm text-red-600">{mappingError}</p> : null}
                {mappingBusy ? <p role="status" className="mb-3 text-sm text-blue-600">연결을 저장하는 중입니다...</p> : null}
                <div ref={mappingContainerRef} className="relative">
                  <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
                    <div ref={templateMappingRef} className="min-w-0 border border-[#e0e0e0] bg-[#fafafa] p-3">
                      <div className="mb-3 flex items-center justify-between gap-2">
                        <span className="text-sm font-bold text-[#0f62fe]">템플릿 화면 · 서식 가져오기</span>
                        <div className="flex items-center gap-2 text-xs">
                          <button type="button" aria-label="템플릿 이전 페이지" disabled={templateMappingPage <= 1} onClick={() => setTemplateMappingPage(p => Math.max(1, p - 1))}><ChevronLeft size={16}/></button>
                          <span>{templateMappingPage} / {templateMappingCount}</span>
                          <button type="button" aria-label="템플릿 다음 페이지" disabled={templateMappingPage >= templateMappingCount} onClick={() => setTemplateMappingPage(p => Math.min(templateMappingCount, p + 1))}><ChevronRight size={16}/></button>
                        </div>
                      </div>
                      <div className="autohwpx-mapping-preview" onScrollCapture={updateConnectorLines}>
                        <ComposePageParagraphLayer
                          paragraphs={visibleTemplateMappingParagraphs} tables={visibleTemplateMappingTables}
                          selectedParagraphId={pendingTemplate?.kind === "paragraph" ? pendingTemplate.id : null}
                          selectedTableId={pendingTemplate?.kind === "table" ? pendingTemplate.id : null}
                          selectedBoxId={null} boxes={[]} isLoading={uploading}
                          linkedParagraphIds={visualMappings.filter(m => m.template.page === templateMappingPage && m.template.kind === "paragraph").map(m => m.template.id)}
                          linkedTableIds={visualMappings.filter(m => m.template.page === templateMappingPage && m.template.kind === "table").map(m => m.template.id)}
                          onSelectParagraph={id => void handleMappingClick("template", "paragraph", id)}
                          onSelectTable={id => void handleMappingClick("template", "table", id)}
                          onSelectBox={() => undefined}
                        />
                      </div>
                    </div>
                    <div ref={sourceMappingRef} className="min-w-0 border border-[#e0e0e0] bg-[#fafafa] p-3">
                      <div className="mb-3 flex items-center justify-between gap-2">
                        <span className="text-sm font-bold text-[#0f62fe]">원본문서 화면 · 서식 적용하기</span>
                        <div className="flex items-center gap-2 text-xs">
                          <button type="button" aria-label="원본문서 이전 페이지" disabled={sourceMappingPage <= 1} onClick={() => setSourceMappingPage(p => Math.max(1, p - 1))}><ChevronLeft size={16}/></button>
                          <span>{sourceMappingPage} / {sourceMappingCount}</span>
                          <button type="button" aria-label="원본문서 다음 페이지" disabled={sourceMappingPage >= sourceMappingCount} onClick={() => setSourceMappingPage(p => Math.min(sourceMappingCount, p + 1))}><ChevronRight size={16}/></button>
                        </div>
                      </div>
                      <div className="autohwpx-mapping-preview" onScrollCapture={updateConnectorLines}>
                        <ComposePageParagraphLayer
                          paragraphs={visibleSourceMappingParagraphs} tables={visibleSourceMappingTables}
                          selectedParagraphId={null} selectedTableId={null} selectedBoxId={null}
                          boxes={[]} isLoading={uploading}
                          linkedParagraphIds={visualMappings.filter(m => m.source.page === sourceMappingPage && m.source.kind === "paragraph").map(m => m.source.id)}
                          linkedTableIds={visualMappings.filter(m => m.source.page === sourceMappingPage && m.source.kind === "table").map(m => m.source.id)}
                          onSelectParagraph={id => void handleMappingClick("source", "paragraph", id)}
                          onSelectTable={id => void handleMappingClick("source", "table", id)}
                          onSelectBox={() => undefined}
                        />
                      </div>
                    </div>
                  </div>
                  {/* SVG uses real DOM rectangles; the layer never intercepts clicks. */}
                  <svg aria-hidden="true" className="pointer-events-none absolute inset-0 hidden h-full w-full overflow-visible lg:block" xmlns="http://www.w3.org/2000/svg">
                    {pendingConnector ? (
                      <g>
                        <path d={`M ${pendingConnector.x1} ${pendingConnector.y1} C ${pendingConnector.x1 + 28} ${pendingConnector.y1}, ${pendingConnector.x2 - 28} ${pendingConnector.y2}, ${pendingConnector.x2} ${pendingConnector.y2}`}
                          stroke="#0f62fe" strokeWidth="2" strokeDasharray="6 5" fill="none" />
                        <circle cx={pendingConnector.x1} cy={pendingConnector.y1} r="5" fill="#0f62fe" stroke="white" strokeWidth="2" />
                      </g>
                    ) : null}
                    {connectorLines.map((line, index) => (
                      <g key={line.label}>
                        <path d={`M ${line.x1} ${line.y1} C ${line.x1 + 30} ${line.y1}, ${line.x2 - 30} ${line.y2}, ${line.x2} ${line.y2}`}
                          fill="none" stroke={index % 2 ? "#e49b0f" : "#0f62fe"} strokeWidth="2.5" />
                        <circle cx={line.x1} cy={line.y1} r="5" fill="#0f62fe" stroke="white" strokeWidth="2" />
                        <circle cx={line.x2} cy={line.y2} r="5" fill="#0f62fe" stroke="white" strokeWidth="2" />
                      </g>
                    ))}
                  </svg>
                </div>
                {visualMappings.length ? (
                  <div className="mt-4 space-y-2">
                    {visualMappings.map((mapping, index) => (
                      <div key={mapping.label} className="flex items-center gap-2 border border-[#e0e0e0] bg-[#f8fbff] px-3 py-2 text-xs">
                        <span className="font-bold text-[#0f62fe]">연결 {index + 1}</span>
                        <span className="min-w-0 flex-1 truncate" title={mapping.template.text}>{mapping.template.text}</span>
                        <span aria-hidden="true">→</span>
                        <span className="min-w-0 flex-1 truncate" title={mapping.source.text}>{mapping.source.text}</span>
                        <span className="shrink-0 text-emerald-700">저장됨</span>
                      </div>
                    ))}
                  </div>
                ) : null}
              </section>

              {/* Previous manual selection UI is retained as an optional advanced fallback. */}
              <details className="mb-6 border border-[#e0e0e0] bg-white p-4">
                <summary className="cursor-pointer text-sm font-semibold text-[#525252]">기존 방식으로 수동 선택하기 (고급)</summary>
                <div className="mt-4">
              <section className="grid gap-6 lg:grid-cols-[1.5fr_0.95fr]">
                <div className="border border-[#e0e0e0] bg-white p-6">
                  <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setActiveSide("template")}
                        className={`rounded-none px-4 py-2 text-sm font-medium transition ${
                          activeSide === "template"
                            ? "bg-[#0f62fe] text-white"
                            : "border border-[#c6c6c6] bg-white text-slate-700 hover:bg-[#f4f4f4]"
                        }`}
                      >
                        템플릿문서
                      </button>

                      <button
                        onClick={() => setActiveSide("source")}
                        className={`rounded-none px-4 py-2 text-sm font-medium transition ${
                          highlightSourceTab
                            ? "animate-pulse bg-yellow-100 text-yellow-900 shadow-[inset_0_0_0_3px_rgba(250,204,21,0.6)]"
                            : activeSide === "source"
                            ? "bg-[#0f62fe] text-white"
                            : "border border-[#c6c6c6] bg-white text-slate-700 hover:bg-[#f4f4f4]"
                        }`}
                      >
                        원본문서
                      </button>
                    </div>

                    <div className="flex items-center gap-2">
                      <div className="group relative inline-flex">
                        <button
                          type="button"
                          onClick={handleClearPreviewBoxes}
                          aria-label="지정된 내용 초기화"
                          aria-describedby="clear-selection-tooltip"
                          className="inline-flex items-center gap-2 rounded-none border border-red-100 bg-red-50 px-3 py-2 text-sm font-semibold text-red-500 transition hover:bg-red-100"
                        >
                          <RefreshCcw className="h-4 w-4" />
                        </button>
                        <span
                          id="clear-selection-tooltip"
                          role="tooltip"
                          className="pointer-events-none absolute bottom-full left-1/2 z-30 mb-2 w-max max-w-[240px] -translate-x-1/2 rounded bg-[#262626] px-3 py-2 text-xs font-medium text-white opacity-0 shadow-md transition-opacity group-hover:opacity-100 group-focus-within:opacity-100"
                        >
                          지정된 내용을 초기화합니다
                        </span>
                      </div>

                      <button
                        onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                        className="rounded-none border border-[#c6c6c6] p-2 hover:bg-[#f4f4f4]"
                      >
                        <ChevronLeft className="h-4 w-4" />
                      </button>

                      <div className="rounded-none border border-[#c6c6c6] px-4 py-2 text-sm font-medium text-slate-700">
                        page {currentPage} / {currentPageCount}
                      </div>

                      <button
                        onClick={() => setCurrentPage((prev) => Math.min(currentPageCount, prev + 1))}
                        className="rounded-none border border-[#c6c6c6] p-2 hover:bg-[#f4f4f4]"
                      >
                        <ChevronRight className="h-4 w-4" />
                      </button>
                    </div>
                  </div>

                  <div className="mb-3 flex items-center gap-2 text-xs font-semibold text-[#0f62fe]">
                    <SquareDashedMousePointer className="h-4 w-4" />
                    미리보기 화면에서 복사하고 싶은 서식을 클릭하세요 
                  </div>

                  <div className="mb-3 pl-6 text-[11px] text-[#6f6f6f]">
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

                      replaceDraftBoxForActiveSide(autoBox)
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

                      replaceDraftBoxForActiveSide(tableBox)
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

                <div className="border border-[#e0e0e0] bg-white p-6">
                  <div className="mb-4 flex items-center gap-2 text-lg font-semibold text-[#161616]">
                    <Tag className="h-5 w-5 text-[#0f62fe]" />
                    선택 패널
                  </div>

                  {uploading ? (
                    <RightPanelLoadingSkeleton />
                  ) : (
                    <>
                      <div className="mb-4 flex items-center gap-2">
                        <button
                          onClick={() => setActivePanel("paragraph")}
                          className={`inline-flex items-center gap-2 rounded-none px-3 py-2 text-sm ${
                            activePanel === "paragraph"
                              ? "bg-[#0f62fe] text-white"
                              : "border border-[#c6c6c6] bg-white text-slate-700"
                          }`}
                        >
                          <AlignLeft className="h-4 w-4" />
                          문단
                        </button>

                        <button
                          onClick={() => setActivePanel("table")}
                          className={`inline-flex items-center gap-2 rounded-none px-3 py-2 text-sm ${
                            activePanel === "table"
                              ? "bg-[#0f62fe] text-white"
                              : "border border-[#c6c6c6] bg-white text-slate-700"
                          }`}
                        >
                          <Table2 className="h-4 w-4" />
                          표
                        </button>
                      </div>

                      <div className="mb-4">
                        <label className="mb-2 block text-sm font-medium text-[#161616]">
                          역할(label)
                        </label>
                        <select
                          value={currentRole}
                          onChange={(e) => {
                            if (activeSide === "template") setTemplateRole(e.target.value)
                            else setSourceRole(e.target.value)
                          }}
                          className="w-full rounded-none border border-[#c6c6c6] bg-white px-4 py-3 text-sm text-[#161616] outline-none"
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
                          <div className="rounded-none border border-dashed border-[#c6c6c6] px-4 py-8 text-center text-sm text-[#525252]">
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
                                  className={`w-full rounded-sm border p-4 text-left transition ${
                                    selected
                                      ? "border-[#1f69ef] bg-[#edf5ff] shadow-none"
                                      : "border-[#e0e0e0] bg-white hover:border-[#bcd2ff] hover:bg-[#f4f4f4]"
                                  }`}
                                >
                                  <div className="mb-2 flex items-center justify-between gap-3">
                                    <span className="rounded-sm bg-white px-2.5 py-1 text-[11px] font-semibold text-[#0f62fe]">
                                      {p.paragraphId}
                                    </span>
                                    <span className="text-[11px] text-[#6f6f6f]">{block.fontInfo}</span>
                                  </div>

                                  <p className="line-clamp-3 text-sm font-medium text-[#161616]">
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

                                  replaceDraftBoxForActiveSide(tableBox)
                                }}
                                className={`w-full rounded-sm border p-4 text-left transition ${
                                  selected
                                    ? "border-[#1f69ef] bg-[#edf5ff] shadow-none"
                                    : "border-[#e0e0e0] bg-white hover:border-[#bcd2ff] hover:bg-[#f4f4f4]"
                                }`}
                              >
                                <div className="mb-2 flex items-center justify-between gap-3">
                                  <span className="rounded-sm bg-white px-2.5 py-1 text-[11px] font-semibold text-[#0f62fe]">
                                    {t.tableId}
                                  </span>
                                  <span className="text-[11px] text-[#6f6f6f]">{block.fontInfo}</span>
                                </div>

                                <p className="line-clamp-2 text-sm font-medium text-[#161616]">
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
                        className="flex w-full items-center justify-center gap-2 rounded-none bg-[#0f62fe] px-4 py-3 text-sm font-semibold text-white shadow-none hover:bg-[#0353e9] disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        <CheckCircle2 className="h-4 w-4" />
                        {saving ? "저장 중..." : "선택 저장"}
                      </button>

                      {savedMessage ? (
                        <div className="mt-4 rounded-none border border-[#c6c6c6] bg-[#edf5ff] px-4 py-3 text-sm text-[#0f62fe]">
                          {savedMessage}
                        </div>
                      ) : null}

                      <div className="mt-4 rounded-none border border-[#e5ecff] bg-[#f4f4f4] px-4 py-3 text-sm text-[#525252]">
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

                </div>
              </details>

              {applyResult ? (
                <section className="mt-6 rounded-none border border-[#e0e0e0] bg-white p-6">
                  <details
                    open={isApplyResultOpen}
                    onToggle={(e) => setIsApplyResultOpen((e.target as HTMLDetailsElement).open)}
                    className="group"
                  >
                    <summary className="cursor-pointer list-none text-xl font-semibold text-[#161616]">
                      <div className="flex items-center justify-between gap-3">
                        <span>apply 결과</span>
                        <span className="text-sm font-medium text-[#525252] group-open:hidden">
                          펼쳐보기
                        </span>
                        <span className="hidden text-sm font-medium text-[#525252] group-open:inline">
                          접기
                        </span>
                      </div>
                    </summary>

                    <pre className="mt-4 overflow-x-auto rounded-none bg-[#0f172a] p-4 text-xs text-slate-100">
                      {JSON.stringify(applyResult, null, 2)}
                    </pre>

                    {applyResult?.downloadUrl ? (
                      <div className="mt-4">
                        <button
                          type="button"
                          onClick={() =>
                            triggerDownload(
                              applyResult.downloadUrl,
                              applyResult.fileId || "output.hwpx"
                            )
                          }
                          className="rounded-none bg-[#0f62fe] px-4 py-2 text-sm font-semibold text-white hover:bg-[#0353e9]"
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

      {/* ✅ 코인 토스트 */}
      {creditToast.show && (
        <div className="fixed bottom-6 right-6 z-[9999] w-[360px] rounded-none border border-slate-700 bg-[#111827] px-5 py-4 text-white shadow-xl">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-none bg-yellow-400 text-lg font-black text-slate-900">
              C
            </div>

            <div className="min-w-0">
              <div className="text-sm font-semibold text-yellow-300">
                Credit Used
              </div>
              <div className="mt-1 text-base font-bold">
                {creditToast.message}
              </div>
              {creditToast.subMessage && (
                <div className="mt-1 text-xs text-slate-300">
                  {creditToast.subMessage}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  )
}