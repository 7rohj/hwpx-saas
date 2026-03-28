import { useMemo, useRef, useState } from "react"

export type OverlayBox = {
  id: string
  x: number
  y: number
  width: number
  height: number
  label?: string

  // 추가:
  // 어떤 paragraph와 연결된 박스인지 저장
  paragraphId?: string
}

type ComposePageOverlayProps = {
  boxes: OverlayBox[]
  selectedBoxId: string | null
  onSelectBox: (id: string) => void
  onCreateBox?: (box: Omit<OverlayBox, "id">) => void
}

export default function ComposePageOverlay({
  boxes,
  selectedBoxId,
  onSelectBox,
  onCreateBox,
}: ComposePageOverlayProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)

  const [dragStart, setDragStart] = useState<{ x: number; y: number } | null>(null)
  const [dragCurrent, setDragCurrent] = useState<{ x: number; y: number } | null>(null)

  const draftBox = useMemo(() => {
    if (!dragStart || !dragCurrent) return null

    const x = Math.min(dragStart.x, dragCurrent.x)
    const y = Math.min(dragStart.y, dragCurrent.y)
    const width = Math.abs(dragCurrent.x - dragStart.x)
    const height = Math.abs(dragCurrent.y - dragStart.y)

    return { x, y, width, height }
  }, [dragStart, dragCurrent])

  function getRelativePoint(clientX: number, clientY: number) {
    const rect = containerRef.current?.getBoundingClientRect()
    if (!rect) return null

    return {
      x: clientX - rect.left,
      y: clientY - rect.top,
    }
  }

  function handleMouseDown(e: React.MouseEvent<HTMLDivElement>) {
    if (!onCreateBox) return
    const point = getRelativePoint(e.clientX, e.clientY)
    if (!point) return

    setDragStart(point)
    setDragCurrent(point)
  }

  function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
    if (!dragStart) return
    const point = getRelativePoint(e.clientX, e.clientY)
    if (!point) return
    setDragCurrent(point)
  }

  function handleMouseUp() {
    if (!onCreateBox || !draftBox) {
      setDragStart(null)
      setDragCurrent(null)
      return
    }

    if (draftBox.width >= 12 && draftBox.height >= 12) {
      onCreateBox({
        x: draftBox.x,
        y: draftBox.y,
        width: draftBox.width,
        height: draftBox.height,
        label: "",
        paragraphId: undefined,
      })
    }

    setDragStart(null)
    setDragCurrent(null)
  }

  return (
    <div
      ref={containerRef}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      className="relative h-[560px] overflow-hidden rounded-[28px] border border-dashed border-[#d7e5ff] bg-[#f8fbff]"
    >
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-semibold text-[#1a2540]">페이지 미리보기</p>
          <p className="mt-2 text-sm text-[#63708a]">
            지금은 오버레이 UX 뼈대 단계입니다.
          </p>
        </div>
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
            className={`absolute rounded-md border-2 text-left transition ${
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
            <span className="absolute -top-7 left-0 rounded-md bg-white px-2 py-1 text-[11px] font-semibold text-slate-700 shadow">
              {box.label?.trim()
                ? `${box.label}${box.paragraphId ? ` · ${box.paragraphId}` : ""}`
                : box.paragraphId ?? box.id}
            </span>
          </button>
        )
      })}

      {draftBox ? (
        <div
          className="absolute rounded-md border-2 border-[#1f69ef] bg-[#1f69ef]/10"
          style={{
            left: draftBox.x,
            top: draftBox.y,
            width: draftBox.width,
            height: draftBox.height,
          }}
        />
      ) : null}
    </div>
  )
}