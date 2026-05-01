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
  spaceBefore: number | null
  spaceAfter: number | null
  lineSpacing: number | null
  singleLine: boolean | null
  paraPrIdRef?: string | null
  charPrIdRef?: string | null
  fontFamily?: string | null
}

type ComposePageCanvasPreviewProps = {
  paragraphs: ParagraphItem[]
  selectedParagraphId: string | null
  boxes: OverlayBox[]
  selectedBoxId: string | null
  onSelectParagraph: (paragraphId: string) => void
  onSelectBox: (boxId: string) => void
}

export default function ComposePageCanvasPreview({
  paragraphs,
  selectedParagraphId,
  boxes,
  selectedBoxId,
  onSelectParagraph,
  onSelectBox,
}: ComposePageCanvasPreviewProps) {
  return (
    <div className="relative h-[560px] overflow-hidden rounded-[28px] border border-[#d7e5ff] bg-[#eef4ff] p-6">
      <div className="mx-auto h-full max-w-[430px] rounded-[18px] border border-slate-200 bg-white px-8 py-8 shadow-[0_18px_45px_rgba(15,23,42,0.10)]">
        <div className="relative h-full overflow-hidden">
          <div className="space-y-2">
            {paragraphs.map((p) => {
              const selected = selectedParagraphId === p.paragraphId
              const fontSize = p.fontSize ?? 12
              const fontWeight = p.bold ? 700 : 400
              const color = p.color ?? "#111111"

              return (
                <button
                  key={p.paragraphId}
                  type="button"
                  onClick={() => onSelectParagraph(p.paragraphId)}
                  className={`block w-full rounded-md px-2 py-1 text-left transition ${
                    selected ? "bg-[#e8f1ff] ring-1 ring-[#1f69ef]" : "hover:bg-slate-50"
                  }`}
                  style={{
                    fontSize: `${Math.max(12, Math.min(fontSize, 24))}px`,
                    fontWeight,
                    color,
                    textAlign:
                      p.align === "center"
                        ? "center"
                        : p.align === "right"
                        ? "right"
                        : "left",
                    lineHeight: 1.45,
                  }}
                >
                  {p.text || "(빈 문단)"}
                  <span className="ml-2 text-[10px] text-slate-400">{p.paragraphId}</span>
                </button>
              )
            })}
          </div>

          {/* 오버레이 박스 */}
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
    </div>
  )
}