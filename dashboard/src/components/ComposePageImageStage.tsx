import ComposePageOverlay, { type OverlayBox } from "./ComposePageOverlay"

type ComposePageImageStageProps = {
  imageUrl: string | null
  boxes: OverlayBox[]
  selectedBoxId: string | null
  onSelectBox: (id: string) => void
  onCreateBox: (box: Omit<OverlayBox, "id">) => void
}

export default function ComposePageImageStage({
  imageUrl,
  boxes,
  selectedBoxId,
  onSelectBox,
  onCreateBox,
}: ComposePageImageStageProps) {
  return (
    <div className="relative h-[560px] overflow-hidden rounded-[28px] border border-[#d7e5ff] bg-[#eef4ff]">
      {imageUrl ? (
        <img
          src={imageUrl}
          alt="page preview"
          className="absolute inset-0 h-full w-full object-contain pointer-events-none select-none"
          draggable={false}
        />
      ) : (
        <div className="absolute inset-0 flex items-center justify-center text-sm text-[#63708a]">
          페이지 이미지 로딩 전
        </div>
      )}

      <div className="absolute inset-0 z-50">
        <ComposePageOverlay
          boxes={boxes}
          selectedBoxId={selectedBoxId}
          onSelectBox={onSelectBox}
          onCreateBox={onCreateBox}
        />
      </div>
    </div>
  )
}