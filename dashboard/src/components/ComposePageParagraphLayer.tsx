import { useMemo } from "react"
import type { OverlayBox } from "./ComposePageOverlay"

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
  onSelectParagraph: (paragraphId: string) => void
  onSelectTable: (tableId: string) => void
  onSelectBox: (boxId: string) => void
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

function toPreviewColor(color?: string | null, fallback = "#111111") {
  if (!color || !color.trim()) return fallback

  let v = color.trim().replace("##", "#")
  if (!v.startsWith("#")) {
    v = `#${v}`
  }

  const hex = v.slice(1)

  if (hex.length === 6) {
    const bb = hex.slice(0, 2)
    const gg = hex.slice(2, 4)
    const rr = hex.slice(4, 6)
    return `#${rr}${gg}${bb}`
  }

  if (hex.length === 8) {
    const aa = hex.slice(0, 2)
    const bb = hex.slice(2, 4)
    const gg = hex.slice(4, 6)
    const rr = hex.slice(6, 8)
    return `#${aa}${rr}${gg}${bb}`
  }

  return v
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
      const bottom = (cell.y ?? 0) + (cell.height ?? 0)
      return Math.max(max, bottom)
    }, 0)

    if (maxBottom > 0) {
      return {
        rowHeights: [],
        renderedTableHeight: maxBottom * 0.5,
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

export default function ComposePageParagraphLayer({
  paragraphs,
  tables,
  selectedParagraphId,
  selectedTableId,
  boxes,
  selectedBoxId,
  onSelectParagraph,
  onSelectTable,
  onSelectBox,
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

  const paragraphBoxMap = useMemo(() => {
    const map = new Map<
      string,
      { left: number; top: number; width: number; height: number }
    >()

    for (const block of renderBlocks) {
      if (block.kind !== "paragraph") continue
      const p = block.paragraph
      map.set(p.paragraphId, getParagraphRenderRect(p, block.displayTop))
    }

    return map
  }, [renderBlocks])

  const tableBoxMap = useMemo(() => {
    const map = new Map<
      string,
      { left: number; top: number; width: number; height: number }
    >()

    for (const block of renderBlocks) {
      if (block.kind !== "table") continue
      const t = block.table

      map.set(t.tableId, {
        left: t.x ?? 56,
        top: block.displayTop,
        width: t.width ?? 420,
        height: block.renderHeight,
      })
    }

    return map
  }, [renderBlocks])

  return (
    <div className="relative h-[720px] w-full overflow-hidden rounded-[24px] border border-[#dbe7ff] bg-[#eef4ff] p-6">
      <div className="relative mx-auto h-full max-w-[560px] rounded-[8px] border border-[#e5e7eb] bg-white shadow-[0_4px_12px_rgba(0,0,0,0.08)]">
        {renderBlocks.map((block) => {
          if (block.kind === "table") {
            const t = block.table
            const style = t.extractedStyle
            const selected = selectedTableId === t.tableId

            const left = t.x ?? 56
            const top = block.displayTop
            const width = t.width ?? 420
            const { rowHeights, renderedTableHeight } = calcTableRenderedHeight(t)

            const rowCount = Math.max(1, t.rowCount ?? t.cellTexts?.length ?? 1)
            const colCount = Math.max(
              1,
              t.colCount ?? Math.max(...(t.cellTexts?.map((r) => r.length) ?? [1]))
            )

            const hasStructuredCells = !!(t.cells && t.cells.length > 0)

            return (
              <button
                key={t.tableId}
                type="button"
                onClick={() => onSelectTable(t.tableId)}
                className={`absolute text-left transition ${
                  selected ? "ring-2 ring-[#1f69ef]" : "hover:bg-slate-50/20"
                }`}
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
                    const textStyle = cell.header
                      ? style?.headerTextStyle
                      : style?.bodyTextStyle

                    const cellLeft = (cell.x ?? 0)
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

                    return (
                      <div
                        key={`${t.tableId}-merged-cell-${idx}`}
                        style={{
                          position: "absolute",
                          left: cellLeft,
                          top: cellTop,
                          width: cellWidth,
                          height: cellHeight,
                          background: cell.header
                            ? toPreviewColor(style?.headerBackgroundColor, "rgba(148,163,184,0.10)")
                            : toPreviewColor(style?.bodyBackgroundColor, "#ffffff"),

                          borderLeft: borders.left,
                          borderRight: borders.right,
                          borderTop: borders.top,
                          borderBottom: borders.bottom,

                          display: "flex",
                          alignItems: "center",
                          justifyContent: toJustifyContent(textStyle?.align),
                          padding: "0 8px",

                          fontFamily: textStyle?.fontFamily ?? t.fontFamily ?? "Pretendard",
                          fontSize: `${textStyle?.fontSize ?? t.fontSize ?? 11}px`,
                          fontWeight: textStyle?.bold ? 700 : cell.header ? 700 : 400,
                          fontStyle: textStyle?.italic ? "italic" : "normal",
                          textDecoration: textStyle?.underline ? "underline" : "none",
                          color: toPreviewColor(textStyle?.fontColor ?? t.fontColor, "#334155"),
                          whiteSpace: "nowrap",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          textAlign: toTextAlign(textStyle?.align),
                          boxSizing: "border-box",
                          pointerEvents: "none",
                        }}
                      >
                        {cell.text}
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
                            const cellText = row[colIdx] ?? ""
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
                                  justifyContent: toJustifyContent(textStyle?.align),
                                  padding: "0 8px",

                                  fontFamily: textStyle?.fontFamily ?? t.fontFamily ?? "Pretendard",
                                  fontSize: `${textStyle?.fontSize ?? t.fontSize ?? 11}px`,
                                  fontWeight: textStyle?.bold ? 700 : isHeader ? 700 : 400,
                                  fontStyle: textStyle?.italic ? "italic" : "normal",
                                  textDecoration: textStyle?.underline ? "underline" : "none",
                                  color: toPreviewColor(textStyle?.fontColor ?? t.fontColor, "#334155"),
                                  whiteSpace: "nowrap",
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  textAlign: toTextAlign(textStyle?.align),
                                  boxSizing: "border-box",
                                  pointerEvents: "none",
                                }}
                              >
                                {cellText}
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
              type="button"
              onClick={() => onSelectParagraph(p.paragraphId)}
              className={`absolute rounded-md px-2 py-1 transition ${
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