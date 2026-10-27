import { useMemo } from "react"
import type { OverlayBox } from "./ComposePageOverlay"
import PreviewLoadingSkeletonWithBot from "./PreviewLoadingSkeletonWithBot"

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
  width?: number | string | null
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
  bodyInnerVerticalBorder?: BorderStyle | null
  bodyInnerHorizontalBorder?: BorderStyle | null
  bodyFirstRowTopBorder?: BorderStyle | null
  bodyLastRowBottomBorder?: BorderStyle | null
}

type TableCellItem = {
  rowIndex: number
  colIndex: number
  rowSpan: number
  colSpan: number
  text: string
  x?: number | null
  y?: number | null
  width?: number | null
  height?: number | null
  header?: boolean | null
  role?: string | null
  backgroundColor?: string | null
  fontFamily?: string | null
  fontSize?: number | null
  bold?: boolean | null
  italic?: boolean | null
  underline?: boolean | null
  fontColor?: string | null
  align?: string | null
}

type TableItem = {
  tableId: string
  page: number | null
  rowCount: number | null
  colCount: number | null
  cellTexts: string[][]
  cells?: TableCellItem[] | null
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

type ComposePageParagraphLayerProps = {
  paragraphs: ParagraphItem[]
  tables: TableItem[]
  selectedParagraphId: string | null
  selectedTableId?: string | null
  selectedBoxId: string | null
  boxes: OverlayBox[]
  isLoading?: boolean
  onSelectParagraph: (paragraphId: string) => void
  onSelectTable: (tableId: string) => void
  onSelectBox: (boxId: string) => void
  linkedParagraphIds?: string[]
  linkedTableIds?: string[]
}

type RenderParagraphBlock = {
  kind: "paragraph"
  y: number
  displayTop: number
  renderHeight: number
  paragraph: ParagraphItem
}

type RenderTableBlock = {
  kind: "table"
  y: number
  displayTop: number
  renderHeight: number
  table: TableItem
}

type RenderBlock = RenderParagraphBlock | RenderTableBlock

const DEFAULT_LEFT_MARGIN = 40
const DEFAULT_PARAGRAPH_WIDTH = 420
const DEFAULT_TABLE_LEFT = 56
const DEFAULT_TABLE_STRETCH_RIGHT_MARGIN = 40

function toPreviewColor(color?: string | null, fallback = "#111111") {
  if (!color || !color.trim()) return fallback

  let v = color.trim().replace("##", "#")
  if (!v.startsWith("#")) {
    v = `#${v}`
  }

  const hex = v.slice(1)

  if (hex.length === 6 || hex.length === 8) {
    return v
  }

  return fallback
}

function toTextAlign(align?: string | null): "left" | "center" | "right" {
  const v = (align ?? "").trim().toLowerCase()
  if (v === "center") return "center"
  if (v === "right") return "right"
  return "left"
}

function toJustifyContent(align?: string | null) {
  const v = (align ?? "").trim().toLowerCase()
  if (v === "center") return "center"
  if (v === "right") return "flex-end"
  return "flex-start"
}

function normalizeBorderWidth(width?: number | string | null, fallback = 1) {
  if (typeof width === "number" && Number.isFinite(width)) {
    return Math.max(0, width)
  }

  if (typeof width === "string") {
    const raw = width.trim().toLowerCase()
    if (!raw || raw === "0") return 0
    if (raw.includes("none")) return 0

    const n = Number.parseFloat(raw.replace(/[^\d.]/g, ""))
    if (Number.isFinite(n)) {
      return Math.max(0.5, Math.min(4, n * 2))
    }
  }

  return fallback
}

function borderEnabled(lineStyle?: string | null) {
  const v = (lineStyle ?? "").trim().toUpperCase()
  if (!v) return true
  return v !== "NONE"
}

function makeBorderCss(
  border?: BorderStyle | null,
  fallbackColor = "#94a3b8",
  fallbackWidth = 1
) {
  const color = toPreviewColor(border?.color, fallbackColor)
  const width = borderEnabled(border?.lineStyle)
    ? normalizeBorderWidth(border?.width, fallbackWidth)
    : 0

  return width > 0 ? `${width}px solid ${color}` : "none"
}

function getCellBorderSet(
  table: TableItem,
  rowIndex: number,
  colIndex: number,
  rowCount: number,
  colCount: number,
  rowSpan = 1,
  colSpan = 1
) {
  const style = table.extractedStyle

  const isHeaderRow = rowIndex === 0
  const endRowIndex = rowIndex + Math.max(1, rowSpan) - 1
  const endColIndex = colIndex + Math.max(1, colSpan) - 1

  const touchesLeft = colIndex === 0
  const touchesRight = endColIndex === colCount - 1
  const touchesBottom = endRowIndex === rowCount - 1

  const headerOuter = style?.headerOuterBorder
  const headerInner = style?.headerInnerBorder

  const bodyOuter = style?.bodyOuterBorder
  const bodyInnerV = style?.bodyInnerVerticalBorder ?? style?.bodyInnerBorder
  const bodyInnerH = style?.bodyInnerHorizontalBorder ?? style?.bodyInnerBorder
  const bodyFirstTop = style?.bodyFirstRowTopBorder ?? bodyInnerH
  const bodyLastBottom = style?.bodyLastRowBottomBorder ?? bodyInnerH

  if (isHeaderRow) {
    return {
      left: touchesLeft ? makeBorderCss(headerOuter) : makeBorderCss(headerInner),
      right: touchesRight ? makeBorderCss(headerOuter) : makeBorderCss(headerInner),
      top: makeBorderCss(headerInner),
      bottom: makeBorderCss(headerInner),
    }
  }

  const isFirstBodyRow = rowIndex === 1

  return {
    left: touchesLeft ? makeBorderCss(bodyOuter) : makeBorderCss(bodyInnerV),
    right: touchesRight ? makeBorderCss(bodyOuter) : makeBorderCss(bodyInnerV),
    top: isFirstBodyRow ? makeBorderCss(bodyFirstTop) : "none",
    bottom: touchesBottom ? makeBorderCss(bodyLastBottom) : makeBorderCss(bodyInnerH),
  }
}

function calcTableRenderedHeight(table: TableItem) {
  if (table.cells && table.cells.length > 0) {
    const maxBottom = table.cells.reduce((max, cell) => {
      const bottom = ((cell.y ?? 0) + (cell.height ?? 0)) * 0.5
      return Math.max(max, bottom)
    }, 0)

    if (maxBottom > 0) {
      return {
        rowHeights: [],
        renderedTableHeight: maxBottom,
      }
    }
  }

  const style = table.extractedStyle
  const rowCount = Math.max(1, table.rowCount ?? table.cellTexts?.length ?? 1)
  const originalHeight = (table.height ?? 120) * 0.5

  const headerFontSize = style?.headerTextStyle?.fontSize ?? table.fontSize ?? 11
  const bodyFontSize = style?.bodyTextStyle?.fontSize ?? table.fontSize ?? 11

  const headerMinHeight = Math.max(30, Math.round((headerFontSize + 10) * 1.5))
  const bodyMinHeight = Math.max(28, Math.round((bodyFontSize + 10) * 1.45))

  const tentativeHeights = Array.from({ length: rowCount }).map((_, idx) =>
    idx === 0 ? headerMinHeight : bodyMinHeight
  )

  const totalMinHeight = tentativeHeights.reduce((sum, v) => sum + v, 0)

  const effectiveHeights =
    totalMinHeight <= originalHeight
      ? (() => {
          const remain = originalHeight - totalMinHeight
          const addPerRow = Math.floor(remain / rowCount)
          const extra = remain % rowCount
          return tentativeHeights.map((h, idx) => h + addPerRow + (idx < extra ? 1 : 0))
        })()
      : (() => {
          const raw = originalHeight / rowCount
          return Array.from({ length: rowCount }).map((_, idx) =>
            idx === rowCount - 1
              ? originalHeight - Math.floor(raw) * (rowCount - 1)
              : Math.floor(raw)
          )
        })()

  return {
    rowHeights: effectiveHeights,
    renderedTableHeight: effectiveHeights.reduce((sum, v) => sum + v, 0),
  }
}

function calcParagraphRenderHeight(p: ParagraphItem) {
  const text = p.text ?? ""
  const lineCount = Math.max(1, text.split(/\r\n|\r|\n/).length)
  const fontSize = p.fontSize ?? 12
  const estimated = lineCount * (fontSize * 1.6 + 6)
  return Math.max(34, Math.min(64, estimated))
}

function getParagraphRenderRect(p: ParagraphItem, displayTop: number) {
  const renderHeight = calcParagraphRenderHeight(p)
  return {
    left: p.x ?? DEFAULT_LEFT_MARGIN,
    top: displayTop,
    width: p.width ?? DEFAULT_PARAGRAPH_WIDTH,
    height: renderHeight,
  }
}

function getStretchedTableRect(
  t: TableItem,
  displayTop: number,
  pageContentWidth: number
) {
  const left = t.x ?? DEFAULT_TABLE_LEFT
  const rightMargin = DEFAULT_TABLE_STRETCH_RIGHT_MARGIN

  const stretchedWidth = Math.max(
    t.width ?? 420,
    pageContentWidth - left - rightMargin
  )

  return {
    left,
    top: displayTop,
    width: stretchedWidth,
    height: calcTableRenderedHeight(t).renderedTableHeight,
  }
}

function getCellTextStyle(table: TableItem, cell: TableCellItem) {
  if (
    cell.fontFamily ||
    cell.fontSize ||
    cell.bold != null ||
    cell.italic != null ||
    cell.underline != null ||
    cell.fontColor ||
    cell.align
  ) {
    return {
      fontFamily: cell.fontFamily ?? table.fontFamily ?? "Pretendard",
      fontSize: cell.fontSize ?? table.fontSize ?? 11,
      bold: cell.bold ?? (cell.header ? true : false),
      italic: cell.italic ?? false,
      underline: cell.underline ?? false,
      fontColor: cell.fontColor ?? table.fontColor ?? "#334155",
      align: cell.align ?? "left",
    }
  }

  const fallback = cell.header
    ? table.extractedStyle?.headerTextStyle
    : table.extractedStyle?.bodyTextStyle

  return {
    fontFamily: fallback?.fontFamily ?? table.fontFamily ?? "Pretendard",
    fontSize: fallback?.fontSize ?? table.fontSize ?? 11,
    bold: fallback?.bold ?? (cell.header ? true : false),
    italic: fallback?.italic ?? false,
    underline: fallback?.underline ?? false,
    fontColor: fallback?.fontColor ?? table.fontColor ?? "#334155",
    align: fallback?.align ?? "left",
  }
}

function getRoleBadgeStyle(role?: string | null) {
  switch ((role ?? "").toUpperCase()) {
    case "HEADER_GROUP":
      return {
        label: "GROUP",
        className: "bg-purple-600 text-white",
      }
    case "HEADER_ROW":
      return {
        label: "ROW",
        className: "bg-blue-600 text-white",
      }
    case "HEADER_COLUMN":
      return {
        label: "COL",
        className: "bg-emerald-600 text-white",
      }
    case "VALUE":
      return {
        label: "VALUE",
        className: "bg-slate-600 text-white",
      }
    default:
      return {
        label: role || "-",
        className: "bg-gray-500 text-white",
      }
  }
}

function renderTextWithSpaceBreaks(text: string) {
  const lines = (text ?? "").split("\n")

  return lines.map((line, lineIdx) => {
    const parts = line.split(" ")
    return (
      <span key={`line-${lineIdx}`}>
        {parts.map((part, idx) => (
          <span key={`part-${lineIdx}-${idx}`}>
            {idx > 0 ? " " : ""}
            {part}
            {idx < parts.length - 1 ? <wbr /> : null}
          </span>
        ))}
        {lineIdx < lines.length - 1 ? <br /> : null}
      </span>
    )
  })
}

function normalizeCellPreviewText(text?: string | null) {
  return (text ?? "")
    .replace(/\r\n|\r|\n/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

/*
function PreviewLoadingSkeleton() {
  const lines = [
    { width: "72%" },
    { width: "88%" },
    { width: "81%" },
    { width: "90%" },
    { width: "64%" },
    { width: "86%" },
    { width: "78%" },
    { width: "92%" },
    { width: "69%" },
    { width: "84%" },
    { width: "74%" },
    { width: "89%" },
  ]

  return (
    <>
      <style>
        {`
          @keyframes previewShimmer {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
          }
        `}
      </style>

      <div className="absolute inset-0 flex items-start justify-center">
        <div className="relative mt-6 h-[640px] w-[480px] rounded-[8px] border border-[#e5e7eb] bg-white shadow-[0_4px_12px_rgba(0,0,0,0.08)]">
          <div className="px-8 pt-8">
            <div
              className="mb-6 h-6 rounded"
              style={{
                width: "46%",
                background:
                  "linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)",
                backgroundSize: "200% 100%",
                animation: "previewShimmer 1.6s ease-in-out infinite",
              }}
            />

            <div className="space-y-3">
              {lines.map((line, idx) => (
                <div
                  key={idx}
                  className="h-3 rounded"
                  style={{
                    width: line.width,
                    background:
                      "linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)",
                    backgroundSize: "200% 100%",
                    animation: "previewShimmer 1.6s ease-in-out infinite",
                    animationDelay: `${idx * 0.05}s`,
                  }}
                />
              ))}
            </div>

            <div className="mt-8 rounded border border-[#dbe3f0] p-4">
              <div className="mb-3 flex gap-2">
                <div
                  className="h-8 rounded"
                  style={{
                    width: "28%",
                    background:
                      "linear-gradient(90deg, #dfe6ef 25%, #f3f4f6 50%, #dfe6ef 75%)",
                    backgroundSize: "200% 100%",
                    animation: "previewShimmer 1.6s ease-in-out infinite",
                  }}
                />
                <div
                  className="h-8 rounded"
                  style={{
                    width: "32%",
                    background:
                      "linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)",
                    backgroundSize: "200% 100%",
                    animation: "previewShimmer 1.6s ease-in-out infinite",
                    animationDelay: "0.08s",
                  }}
                />
              </div>

              {[...Array(5)].map((_, rowIdx) => (
                <div key={rowIdx} className="mb-2 flex gap-2">
                  <div
                    className="h-7 rounded"
                    style={{
                      width: "24%",
                      background:
                        "linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)",
                      backgroundSize: "200% 100%",
                      animation: "previewShimmer 1.6s ease-in-out infinite",
                      animationDelay: `${rowIdx * 0.06}s`,
                    }}
                  />
                  <div
                    className="h-7 rounded"
                    style={{
                      width: "74%",
                      background:
                        "linear-gradient(90deg, #eef2f7 25%, #f8fafc 50%, #eef2f7 75%)",
                      backgroundSize: "200% 100%",
                      animation: "previewShimmer 1.6s ease-in-out infinite",
                      animationDelay: `${rowIdx * 0.08}s`,
                    }}
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
*/

export default function ComposePageParagraphLayer({
  paragraphs,
  tables,
  selectedParagraphId,
  selectedTableId,
  selectedBoxId,
  boxes,
  isLoading = false,
  onSelectParagraph,
  onSelectTable,
  onSelectBox,
  linkedParagraphIds = [],
  linkedTableIds = [],
}: ComposePageParagraphLayerProps) {
  const renderBlocks = useMemo<RenderBlock[]>(() => {
    const rawBlocks: RenderBlock[] = [
      ...tables.map((t) => ({
        kind: "table" as const,
        y: t.y ?? 0,
        displayTop: t.y ?? 0,
        renderHeight: calcTableRenderedHeight(t).renderedTableHeight,
        table: t,
      })),
      ...paragraphs.map((p) => ({
        kind: "paragraph" as const,
        y: p.y ?? 0,
        displayTop: p.y ?? 0,
        renderHeight: calcParagraphRenderHeight(p),
        paragraph: p,
      })),
    ].sort((a, b) => a.y - b.y)

    if (rawBlocks.length === 0) {
      return rawBlocks
    }

    const first = rawBlocks[0]
    first.displayTop = first.y

    for (let i = 1; i < rawBlocks.length; i++) {
      const prev = rawBlocks[i - 1]
      const curr = rawBlocks[i]

      const prevOriginalBottom = prev.y + prev.renderHeight
      const originalGap = Math.max(0, curr.y - prevOriginalBottom)
      const compactedGap = originalGap * 0.2

      const prevDisplayBottom = prev.displayTop + prev.renderHeight
      curr.displayTop = prevDisplayBottom + compactedGap
    }

    return rawBlocks
  }, [paragraphs, tables])

  const pageContentWidth = useMemo(() => {
    let maxRight = 480

    for (const block of renderBlocks) {
      if (block.kind === "paragraph") {
        const p = block.paragraph
        const left = p.x ?? DEFAULT_LEFT_MARGIN
        const width = p.width ?? DEFAULT_PARAGRAPH_WIDTH
        maxRight = Math.max(maxRight, left + width)
      } else {
        const t = block.table
        const left = t.x ?? DEFAULT_TABLE_LEFT
        const width = t.width ?? 420
        maxRight = Math.max(maxRight, left + width)
      }
    }

    return maxRight + 32
  }, [renderBlocks])

  const paragraphBoxMap = useMemo(() => {
    const map = new Map<string, { left: number; top: number; width: number; height: number }>()

    for (const block of renderBlocks) {
      if (block.kind !== "paragraph") continue
      const p = block.paragraph
      map.set(p.paragraphId, getParagraphRenderRect(p, block.displayTop))
    }

    return map
  }, [renderBlocks])

  const tableBoxMap = useMemo(() => {
    const map = new Map<string, { left: number; top: number; width: number; height: number }>()

    for (const block of renderBlocks) {
      if (block.kind !== "table") continue
      const t = block.table
      const rect = getStretchedTableRect(t, block.displayTop, pageContentWidth)
      map.set(t.tableId, rect)
    }

    return map
  }, [renderBlocks, pageContentWidth])

  return (
    <div className="relative h-[720px] w-full overflow-y-auto overflow-x-auto rounded-[24px] border border-[#dbe7ff] bg-[#eef4ff] p-6">
      {/* {isLoading ? <PreviewLoadingSkeleton /> : null} */}
      {isLoading ? <PreviewLoadingSkeletonWithBot /> : null}

      <div
        className={`relative mx-auto min-h-full rounded-[8px] border border-[#e5e7eb] bg-white shadow-[0_4px_12px_rgba(0,0,0,0.08)] ${
          isLoading ? "pointer-events-none opacity-0" : "opacity-100"
        }`}
        style={{ width: `${pageContentWidth}px` }}
      >
        {renderBlocks.map((block) => {
          if (block.kind === "table") {
            const t = block.table
            const style = t.extractedStyle
            const selected = selectedTableId === t.tableId

            const tableRect = getStretchedTableRect(t, block.displayTop, pageContentWidth)
            const left = tableRect.left
            const top = tableRect.top
            const width = tableRect.width
            const renderedTableHeight = tableRect.height
            const { rowHeights } = calcTableRenderedHeight(t)

            const rowCount = Math.max(1, t.rowCount ?? t.cellTexts?.length ?? 1)
            const colCount = Math.max(
              1,
              t.colCount ?? Math.max(...(t.cellTexts?.map((r) => r.length) ?? [1]))
            )

            const hasStructuredCells = !!(t.cells && t.cells.length > 0)

            return (
              <button
                key={t.tableId}
                data-compose-id={t.tableId}
                data-compose-kind="table"
                type="button"
                onClick={() => onSelectTable(t.tableId)}
                className={`absolute text-left transition ${
                  selected ? "ring-2 ring-[#1f69ef]" : "hover:bg-slate-50/20"
                } ${linkedTableIds.includes(t.tableId) ? "autohwpx-linked-block" : ""}`}
                style={{
                  left,
                  top,
                  width,
                  height: renderedTableHeight,
                  zIndex: 10,
                  background: "transparent",
                }}
              >
                {hasStructuredCells ? (
                  t.cells!.map((cell, idx) => {
                    const textStyle = getCellTextStyle(t, cell)
                    const badge = getRoleBadgeStyle(cell.role)

                    const cellLeft = cell.x ?? 0
                    const cellTop = (cell.y ?? 0) * 0.5
                    const cellWidth = cell.width ?? 0
                    const cellHeight = (cell.height ?? 0) * 0.5

                    const borders = getCellBorderSet(
                      t,
                      cell.rowIndex,
                      cell.colIndex,
                      rowCount,
                      colCount,
                      cell.rowSpan,
                      cell.colSpan
                    )

                    const resolvedBg =
                      cell.backgroundColor != null
                        ? toPreviewColor(cell.backgroundColor, "#ffffff")
                        : cell.header
                        ? toPreviewColor(style?.headerBackgroundColor, "rgba(148,163,184,0.10)")
                        : toPreviewColor(style?.bodyBackgroundColor, "#ffffff")

                    const previewText = normalizeCellPreviewText(cell.text)

                    return (
                      <div
                        key={`${t.tableId}-merged-cell-${idx}`}
                        style={{
                          position: "absolute",
                          left: cellLeft,
                          top: cellTop,
                          width: cellWidth,
                          height: cellHeight,
                          background: resolvedBg,

                          borderLeft: borders.left,
                          borderRight: borders.right,
                          borderTop: borders.top,
                          borderBottom: borders.bottom,

                          display: "flex",
                          alignItems: "center",
                          justifyContent: toJustifyContent(textStyle.align),
                          padding: "4px 8px",

                          fontFamily: textStyle.fontFamily,
                          fontSize: `${textStyle.fontSize}px`,
                          fontWeight: textStyle.bold ? 700 : 400,
                          fontStyle: textStyle.italic ? "italic" : "normal",
                          textDecoration: textStyle.underline ? "underline" : "none",
                          color: toPreviewColor(textStyle.fontColor, "#334155"),
                          whiteSpace: "normal",
                          wordBreak: "keep-all",
                          overflowWrap: "break-word",
                          overflow: "hidden",
                          textAlign: toTextAlign(textStyle.align),
                          boxSizing: "border-box",
                          pointerEvents: "none",
                          lineHeight: 1.3,
                        }}
                      >
                        {renderTextWithSpaceBreaks(previewText)}

                        <span
                          className={`absolute right-1 top-1 rounded px-1 py-[1px] text-[9px] font-bold leading-none shadow-sm ${badge.className}`}
                          style={{ pointerEvents: "none" }}
                        >
                          {badge.label}
                        </span>
                      </div>
                    )
                  })
                ) : (
                  (() => {
                    const tableRows = t.cellTexts ?? []

                    const colWidths = Array.from({ length: colCount }).map((_, idx) => {
                      const raw = width / colCount
                      if (idx === colCount - 1) {
                        return width - Math.floor(raw) * (colCount - 1)
                      }
                      return Math.floor(raw)
                    })

                    let currentTop = 0

                    return Array.from({ length: rowCount }).map((_, rowIdx) => {
                      const row = tableRows[rowIdx] ?? []
                      const isHeader = rowIdx === 0
                      const textStyle = isHeader
                        ? style?.headerTextStyle
                        : style?.bodyTextStyle

                      const rowHeight = rowHeights[rowIdx]
                      const rowTop = currentTop
                      currentTop += rowHeight

                      let currentLeft = 0

                      return (
                        <div
                          key={`${t.tableId}-row-${rowIdx}`}
                          className="absolute left-0 right-0"
                          style={{
                            top: rowTop,
                            height: rowHeight,
                          }}
                        >
                          {Array.from({ length: colCount }).map((_, colIdx) => {
                            const cellText = normalizeCellPreviewText(row[colIdx] ?? "")
                            const cellWidth = colWidths[colIdx]
                            const cellLeft = currentLeft
                            currentLeft += cellWidth

                            const borders = getCellBorderSet(t, rowIdx, colIdx, rowCount, colCount)

                            return (
                              <div
                                key={`${t.tableId}-cell-${rowIdx}-${colIdx}`}
                                style={{
                                  position: "absolute",
                                  left: cellLeft,
                                  top: 0,
                                  width: cellWidth,
                                  height: rowHeight,
                                  background: isHeader
                                    ? toPreviewColor(style?.headerBackgroundColor, "rgba(148,163,184,0.10)")
                                    : toPreviewColor(style?.bodyBackgroundColor, "#ffffff"),

                                  borderLeft: borders.left,
                                  borderRight: borders.right,
                                  borderTop: borders.top,
                                  borderBottom: borders.bottom,

                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  padding: "4px 8px",

                                  fontFamily: textStyle?.fontFamily ?? t.fontFamily ?? "Pretendard",
                                  fontSize: `${textStyle?.fontSize ?? t.fontSize ?? 11}px`,
                                  fontWeight: textStyle?.bold ? 700 : isHeader ? 700 : 400,
                                  fontStyle: textStyle?.italic ? "italic" : "normal",
                                  textDecoration: textStyle?.underline ? "underline" : "none",
                                  color: toPreviewColor(textStyle?.fontColor ?? t.fontColor, "#334155"),

                                  whiteSpace: "normal",
                                  wordBreak: "keep-all",
                                  overflowWrap: "break-word",
                                  writingMode: "horizontal-tb",
                                  overflow: "hidden",
                                  textAlign: "center",
                                  boxSizing: "border-box",
                                  pointerEvents: "none",
                                  lineHeight: 1.3,
                                }}
                              >
                                {renderTextWithSpaceBreaks(cellText)}
                              </div>
                            )
                          })}
                        </div>
                      )
                    })
                  })()
                )}

                <div className="absolute left-0 top-0 rounded-br-md bg-white/90 px-2 py-0.5 text-[10px] font-semibold text-slate-700 shadow-sm pointer-events-none">
                  {t.tableId}
                </div>
              </button>
            )
          }

          const p = block.paragraph
          const selected = selectedParagraphId === p.paragraphId
          const rect = getParagraphRenderRect(p, block.displayTop)

          return (
            <button
              key={p.paragraphId}
              data-compose-id={p.paragraphId}
              data-compose-kind="paragraph"
              type="button"
              onClick={() => onSelectParagraph(p.paragraphId)}
              className={`absolute rounded-md px-2 py-1 transition ${linkedParagraphIds.includes(p.paragraphId) ? "autohwpx-linked-block" : ""} ${
                selected
                  ? "bg-[#e8f1ff] ring-1 ring-[#1f69ef]"
                  : "hover:bg-slate-50"
              } ${(p.fontSize ?? 12) >= 14 ? "font-semibold" : ""}`}
              style={{
                left: rect.left,
                top: rect.top,
                width: rect.width,
                height: rect.height,
                fontSize: `${p.fontSize ?? 12}px`,
                fontWeight: p.bold ? 700 : 400,
                fontStyle: p.italic ? "italic" : "normal",
                textDecoration: p.underline ? "underline" : "none",
                color: toPreviewColor(p.color, "#111111"),
                textAlign: toTextAlign(p.align),
                lineHeight: 1.6,
                fontFamily: p.fontFamily ?? "Pretendard",
                whiteSpace: "pre-wrap",
                overflow: "hidden",
                zIndex: 20,
              }}
            >
              {p.text || "(빈 문단)"}
            </button>
          )
        })}

        {boxes.map((box) => {
          const selected = selectedBoxId === box.id

          let resolvedLeft = box.x
          let resolvedTop = box.y
          let resolvedWidth = box.width
          let resolvedHeight = box.height

          if (box.paragraphId) {
            const rect = paragraphBoxMap.get(box.paragraphId)
            if (rect) {
              resolvedLeft = rect.left
              resolvedTop = rect.top
              resolvedWidth = rect.width
              resolvedHeight = rect.height
            }
          } else {
            const matchedTableId = Array.from(tableBoxMap.keys()).find(
              (tableId) => box.id === `${tableId}-box`
            )

            if (matchedTableId) {
              const rect = tableBoxMap.get(matchedTableId)
              if (rect) {
                resolvedLeft = rect.left
                resolvedTop = rect.top
                resolvedWidth = rect.width
                resolvedHeight = rect.height
              }
            }
          }

          return (
            <button
              key={box.id}
              type="button"
              onClick={(e) => {
                e.stopPropagation()
                onSelectBox(box.id)
              }}
              className={`absolute rounded-md border-2 transition ${
                selected
                  ? "border-[#1f69ef] bg-[#1f69ef]/10"
                  : "border-red-500 bg-red-500/10 hover:bg-red-500/15"
              }`}
              style={{
                left: resolvedLeft,
                top: resolvedTop,
                width: resolvedWidth,
                height: resolvedHeight,
                zIndex: 30,
              }}
            >
              <span className="absolute -top-6 left-0 rounded bg-white px-1.5 py-0.5 text-[10px] font-semibold text-slate-700 shadow">
                {box.label || box.id}
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}