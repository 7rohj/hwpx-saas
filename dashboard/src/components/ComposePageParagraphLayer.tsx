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

type ComposePageParagraphLayerProps = {
  paragraphs: ParagraphItem[]
  tables: TableItem[]
  selectedParagraphId: string | null
  selectedBoxId: string | null
  boxes: OverlayBox[]
  onSelectParagraph: (paragraphId: string) => void
  onSelectBox: (boxId: string) => void
}

/**
 * HWPX 색상은 내부적으로 BGR 계열로 들어오는 경우가 있어서
 * preview(CSS)에서는 RGB로 다시 뒤집어 보여준다.
 *
 * 예:
 *   #00D7FF -> #FFD700
 *   #5B81D5 -> #D5815B
 */
function toPreviewColor(color?: string | null, fallback = "#111111") {
  if (!color || !color.trim()) return fallback

  let v = color.trim().replace("##", "#")
  if (!v.startsWith("#")) {
    v = `#${v}`
  }

  const hex = v.slice(1)

  // #RRGGBB 로 보이지만 실제는 #BBGGRR 로 저장된 값을 preview용으로 뒤집음
  if (hex.length === 6) {
    const bb = hex.slice(0, 2)
    const gg = hex.slice(2, 4)
    const rr = hex.slice(4, 6)
    return `#${rr}${gg}${bb}`
  }

  // #AABBGGRR -> #AARRGGBB
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
  colCount: number
) {
  const style = table.extractedStyle

  const isHeaderRow = rowIndex === 0
  const isSingleCol = colCount === 1
  const isFirstCol = colIndex === 0
  const isLastCol = colIndex === colCount - 1

  const hasBody = rowCount >= 2
  const isOnlyOneBodyRow = rowCount === 2 && rowIndex === 1
  const isFirstBodyRow = rowIndex === 1 && rowCount > 2
  const isLastBodyRow = rowIndex === rowCount - 1 && rowIndex > 1
  const isMiddleBodyRow = rowIndex > 1 && rowIndex < rowCount - 1

  const headerOuter = style?.headerOuterBorder
  const headerInner = style?.headerInnerBorder

  const bodyOuter = style?.bodyOuterBorder
  const bodyInnerV = style?.bodyInnerVerticalBorder ?? style?.bodyInnerBorder
  const bodyInnerH = style?.bodyInnerHorizontalBorder ?? style?.bodyInnerBorder
  const bodyFirstTop = style?.bodyFirstRowTopBorder ?? bodyInnerH
  const bodyLastBottom = style?.bodyLastRowBottomBorder ?? bodyInnerH

  if (isHeaderRow) {
    if (isSingleCol) {
      return {
        left: makeBorderCss(headerOuter),
        right: makeBorderCss(headerOuter),
        top: makeBorderCss(headerInner),
        bottom: makeBorderCss(headerInner),
      }
    }

    if (isFirstCol) {
      return {
        left: makeBorderCss(headerOuter),
        right: makeBorderCss(headerInner),
        top: makeBorderCss(headerInner),
        bottom: makeBorderCss(headerInner),
      }
    }

    if (isLastCol) {
      return {
        left: makeBorderCss(headerInner),
        right: makeBorderCss(headerOuter),
        top: makeBorderCss(headerInner),
        bottom: makeBorderCss(headerInner),
      }
    }

    return {
      left: makeBorderCss(headerInner),
      right: makeBorderCss(headerInner),
      top: makeBorderCss(headerInner),
      bottom: makeBorderCss(headerInner),
    }
  }

  if (!hasBody) {
    return {
      left: makeBorderCss(bodyOuter),
      right: makeBorderCss(bodyOuter),
      top: makeBorderCss(bodyFirstTop),
      bottom: makeBorderCss(bodyLastBottom),
    }
  }

  if (isOnlyOneBodyRow) {
    if (isSingleCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyOuter),
        top: makeBorderCss(bodyFirstTop),
        bottom: makeBorderCss(bodyLastBottom),
      }
    }

    if (isFirstCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyInnerV),
        top: makeBorderCss(bodyFirstTop),
        bottom: makeBorderCss(bodyLastBottom),
      }
    }

    if (isLastCol) {
      return {
        left: makeBorderCss(bodyInnerV),
        right: makeBorderCss(bodyOuter),
        top: makeBorderCss(bodyFirstTop),
        bottom: makeBorderCss(bodyLastBottom),
      }
    }

    return {
      left: makeBorderCss(bodyInnerV),
      right: makeBorderCss(bodyInnerV),
      top: makeBorderCss(bodyFirstTop),
      bottom: makeBorderCss(bodyLastBottom),
    }
  }

  if (isFirstBodyRow) {
    if (isSingleCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyOuter),
        top: makeBorderCss(bodyFirstTop),
        bottom: makeBorderCss(bodyInnerH),
      }
    }

    if (isFirstCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyInnerV),
        top: makeBorderCss(bodyFirstTop),
        bottom: makeBorderCss(bodyInnerH),
      }
    }

    if (isLastCol) {
      return {
        left: makeBorderCss(bodyInnerV),
        right: makeBorderCss(bodyOuter),
        top: makeBorderCss(bodyFirstTop),
        bottom: makeBorderCss(bodyInnerH),
      }
    }

    return {
      left: makeBorderCss(bodyInnerV),
      right: makeBorderCss(bodyInnerV),
      top: makeBorderCss(bodyFirstTop),
      bottom: makeBorderCss(bodyInnerH),
    }
  }

  if (isMiddleBodyRow) {
    if (isSingleCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyOuter),
        top: "none",
        bottom: makeBorderCss(bodyInnerH),
      }
    }

    if (isFirstCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyInnerV),
        top: "none",
        bottom: makeBorderCss(bodyInnerH),
      }
    }

    if (isLastCol) {
      return {
        left: makeBorderCss(bodyInnerV),
        right: makeBorderCss(bodyOuter),
        top: "none",
        bottom: makeBorderCss(bodyInnerH),
      }
    }

    return {
      left: makeBorderCss(bodyInnerV),
      right: makeBorderCss(bodyInnerV),
      top: "none",
      bottom: makeBorderCss(bodyInnerH),
    }
  }

  if (isLastBodyRow) {
    if (isSingleCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyOuter),
        top: "none",
        bottom: makeBorderCss(bodyLastBottom),
      }
    }

    if (isFirstCol) {
      return {
        left: makeBorderCss(bodyOuter),
        right: makeBorderCss(bodyInnerV),
        top: "none",
        bottom: makeBorderCss(bodyLastBottom),
      }
    }

    if (isLastCol) {
      return {
        left: makeBorderCss(bodyInnerV),
        right: makeBorderCss(bodyOuter),
        top: "none",
        bottom: makeBorderCss(bodyLastBottom),
      }
    }

    return {
      left: makeBorderCss(bodyInnerV),
      right: makeBorderCss(bodyInnerV),
      top: "none",
      bottom: makeBorderCss(bodyLastBottom),
    }
  }

  return {
    left: makeBorderCss(bodyInnerV),
    right: makeBorderCss(bodyInnerV),
    top: makeBorderCss(bodyInnerH),
    bottom: makeBorderCss(bodyInnerH),
  }
}

export default function ComposePageParagraphLayer({
  paragraphs,
  tables,
  selectedParagraphId,
  boxes,
  selectedBoxId,
  onSelectParagraph,
  onSelectBox,
}: ComposePageParagraphLayerProps) {
  const renderBlocks = [
    ...tables.map((t) => ({
      kind: "table" as const,
      y: t.y ?? 0,
      table: t,
    })),
    ...paragraphs.map((p) => ({
      kind: "paragraph" as const,
      y: p.y ?? 0,
      paragraph: p,
    })),
  ].sort((a, b) => a.y - b.y)

  return (
    <div className="relative h-[720px] w-full overflow-hidden rounded-[24px] border border-[#dbe7ff] bg-[#eef4ff] p-6">
      <div className="relative mx-auto h-full max-w-[560px] rounded-[8px] border border-[#e5e7eb] bg-white shadow-[0_4px_12px_rgba(0,0,0,0.08)]">
        {renderBlocks.map((block) => {
          if (block.kind === "table") {
            const t = block.table
            const style = t.extractedStyle

            const left = t.x ?? 56
            const top = t.y ?? 80
            const width = t.width ?? 420
            const originalHeight = t.height ?? 120

            const rowCount = Math.max(1, t.rowCount ?? t.cellTexts?.length ?? 1)
            const colCount = Math.max(
              1,
              t.colCount ?? Math.max(...(t.cellTexts?.map((r) => r.length) ?? [1]))
            )

            const tableRows = t.cellTexts ?? []

            const headerFontSize = style?.headerTextStyle?.fontSize ?? t.fontSize ?? 11
            const bodyFontSize = style?.bodyTextStyle?.fontSize ?? t.fontSize ?? 11

            /**
             * 요구사항:
             * preview에서는 표의 각 셀 세로 길이를 기존 계산값의 1/2로 강제 보정합니다.
             * 그래서 row 높이 산출에 사용할 전체 높이도 1/2로 먼저 줄여서 계산합니다.
             */
            const compactedTableHeight = originalHeight * 0.5

            const headerMinHeight = Math.max(30, Math.round((headerFontSize + 10) * 1.5))
            const bodyMinHeight = Math.max(28, Math.round((bodyFontSize + 10) * 1.45))

            const tentativeHeights = Array.from({ length: rowCount }).map((_, idx) =>
              idx === 0 ? headerMinHeight : bodyMinHeight
            )

            const totalMinHeight = tentativeHeights.reduce((sum, v) => sum + v, 0)
            const effectiveHeights =
              totalMinHeight <= compactedTableHeight
                ? (() => {
                    const remain = compactedTableHeight - totalMinHeight
                    const addPerRow = Math.floor(remain / rowCount)
                    const extra = remain % rowCount
                    return tentativeHeights.map((h, idx) => h + addPerRow + (idx < extra ? 1 : 0))
                  })()
                : (() => {
                    const raw = compactedTableHeight / rowCount
                    return Array.from({ length: rowCount }).map((_, idx) =>
                      idx === rowCount - 1
                        ? compactedTableHeight - Math.floor(raw) * (rowCount - 1)
                        : Math.floor(raw)
                    )
                  })()

            const renderedTableHeight = effectiveHeights.reduce((sum, v) => sum + v, 0)

            const colWidths = Array.from({ length: colCount }).map((_, idx) => {
              const raw = width / colCount
              if (idx === colCount - 1) {
                return width - Math.floor(raw) * (colCount - 1)
              }
              return Math.floor(raw)
            })

            let currentTop = 0

            return (
              <div
                key={t.tableId}
                className="absolute"
                style={{
                  left,
                  top,
                  width,
                  height: renderedTableHeight,
                  zIndex: 10,
                }}
              >
                {Array.from({ length: rowCount }).map((_, rowIdx) => {
                  const row = tableRows[rowIdx] ?? []
                  const isHeader = rowIdx === 0
                  const textStyle = isHeader
                    ? style?.headerTextStyle
                    : style?.bodyTextStyle

                  const rowHeight = effectiveHeights[rowIdx]
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
                            }}
                          >
                            {cellText}
                          </div>
                        )
                      })}
                    </div>
                  )
                })}

                <div className="absolute left-0 top-0 rounded-br-md bg-white/90 px-2 py-0.5 text-[10px] font-semibold text-slate-700 shadow-sm">
                  {t.tableId}
                </div>
              </div>
            )
          }

          const p = block.paragraph
          const selected = selectedParagraphId === p.paragraphId

          return (
            <button
              key={p.paragraphId}
              type="button"
              onClick={() => onSelectParagraph(p.paragraphId)}
              className={`absolute rounded-md px-2 py-1 text-left transition ${
                selected
                  ? "bg-[#e8f1ff] ring-1 ring-[#1f69ef]"
                  : "hover:bg-slate-50"
              } ${(p.fontSize ?? 12) >= 14 ? "font-semibold" : ""}`}
              style={{
                left: p.x ?? 40,
                top: p.y ?? 40,
                width: p.width ?? 420,
                height: p.height ?? 34,
                fontSize: `${p.fontSize ?? 12}px`,
                fontWeight: p.bold ? 700 : 400,
                fontStyle: p.italic ? "italic" : "normal",
                textDecoration: p.underline ? "underline" : "none",
                color: toPreviewColor(p.color, "#111111"),
                textAlign:
                  p.align === "center"
                    ? "center"
                    : p.align === "right"
                    ? "right"
                    : "left",
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
                left: box.x,
                top: box.y,
                width: box.width,
                height: box.height,
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