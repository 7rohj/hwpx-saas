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
}

type ComposePageParagraphLayerProps = {
  paragraphs: ParagraphItem[]

  // 추가:
  // 표도 함께 받아서 좌측 페이지에 렌더링
  tables: TableItem[]

  selectedParagraphId: string | null
  selectedBoxId: string | null
  boxes: OverlayBox[]
  onSelectParagraph: (paragraphId: string) => void
  onSelectBox: (boxId: string) => void
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
  // 추가:
  // 문단/표를 y 기준으로 한 리스트로 섞어서 렌더링
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

            const left = t.x ?? 56
            const top = t.y ?? 80
            const width = t.width ?? 420
            const height = t.height ?? 120

            const borderColor = t.borderColor ?? "#94a3b8"
            const borderWidth = t.borderWidth ?? 1
            const bgColor = t.backgroundColor ?? "#ffffff"

            const rowCount = Math.max(1, t.rowCount ?? 1)
            const colCount = Math.max(1, t.colCount ?? 1)

            const previewRows = t.cellTexts?.slice(0, Math.min(rowCount, 4)) ?? []
            const rowHeight = Math.max(28, Math.floor(height / rowCount))
            const colWidth = Math.max(60, Math.floor(width / colCount))

            return (
              <div
                key={t.tableId}
                className="absolute overflow-hidden rounded-[2px]"
                style={{
                  left,
                  top,
                  width,
                  height,
                  background: bgColor,
                  border: `${borderWidth}px solid ${borderColor}`,
                  zIndex: 10,
                }}
              >
                {/* 추가:
                    간단한 표 grid 렌더링 */}
                {previewRows.map((row, rowIndex) => (
                  <div
                    key={`${t.tableId}-row-${rowIndex}`}
                    className="absolute left-0 right-0 flex"
                    style={{
                      top: rowIndex * rowHeight,
                      height: rowHeight,
                      borderBottom:
                        rowIndex === previewRows.length - 1
                          ? "none"
                          : `${borderWidth}px solid ${borderColor}`,
                      background:
                        rowIndex === 0
                          ? "rgba(148,163,184,0.10)"
                          : "transparent",
                    }}
                  >
                    {Array.from({ length: colCount }).map((_, colIndex) => {
                      const cellText = row?.[colIndex] ?? ""

                      return (
                        <div
                          key={`${t.tableId}-cell-${rowIndex}-${colIndex}`}
                          className="flex items-center px-2 text-[11px] text-slate-700"
                          style={{
                            width: colWidth,
                            borderRight:
                              colIndex === colCount - 1
                                ? "none"
                                : `${borderWidth}px solid ${borderColor}`,
                            fontWeight: rowIndex === 0 ? 700 : 400,
                            overflow: "hidden",
                            whiteSpace: "nowrap",
                            textOverflow: "ellipsis",
                          }}
                        >
                          {cellText}
                        </div>
                      )
                    })}
                  </div>
                ))}

                {/* 추가:
                    표 ID 뱃지 */}
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
                color: p.color ?? "#111111",
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

        {/* 기존:
            오버레이 박스 */}
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