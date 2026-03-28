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

type OverlayBox = {
  id: string
  x: number
  y: number
  width: number
  height: number
  label?: string
  paragraphId?: string
}

type ComposePageParagraphLayerProps = {
  paragraphs: ParagraphItem[]
  selectedParagraphId: string | null
  boxes: OverlayBox[]
  selectedBoxId: string | null
  onSelectParagraph: (paragraphId: string) => void
  onSelectBox: (boxId: string) => void
}

export default function ComposePageParagraphLayer({
  paragraphs,
  selectedParagraphId,
  boxes,
  selectedBoxId,
  onSelectParagraph,
  onSelectBox,
}: ComposePageParagraphLayerProps) {
  return (
    <div className="relative h-[720px] w-full overflow-hidden rounded-[24px] border border-[#dbe7ff] bg-[#eef4ff] p-6">
      <div className="relative mx-auto h-full max-w-[560px] rounded-[16px] border border-slate-200 bg-white shadow-[0_18px_45px_rgba(15,23,42,0.10)]">
        <div className="p-10"> {/* 추가 */}
        {paragraphs.map((p) => {
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
                } ${
                (p.fontSize ?? 12) >= 14 ? "font-semibold" : ""
              }`}              
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
            }}
            >
              {p.text || "(빈 문단)"}
            </button>
          )
        })}
        </div>

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
  )
}