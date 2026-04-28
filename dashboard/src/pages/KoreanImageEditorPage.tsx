import { useEffect, useRef, useState } from "react"
import { Canvas, FabricImage, Point, Rect, Textbox } from "fabric"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"

type KoreanImageEditorPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

type RepairResponse = {
  backgroundImageDataUrl: string
  textLayer: {
    text: string
    x: number
    y: number
    fontSize: number
    color: string
    fontFamily: string
  }
}

export default function KoreanImageEditorPage({
  onNavigate,
  onLogout,
}: KoreanImageEditorPageProps) {
  const canvasElRef = useRef<HTMLCanvasElement | null>(null)
  const fabricRef = useRef<Canvas | null>(null)

  const [text, setText] = useState("봄맞이 할인 이벤트")
  const [fontSize, setFontSize] = useState(42)
  const [color, setColor] = useState("#111111")
  const [repairing, setRepairing] = useState(false)

  useEffect(() => {
    if (!canvasElRef.current) return

    const canvas = new Canvas(canvasElRef.current, {
      width: 900,
      height: 620,
      backgroundColor: "#f4f4f4",
      preserveObjectStacking: true,
      selection: true,
    })

    fabricRef.current = canvas

    let isDragging = false
    let lastPosX = 0
    let lastPosY = 0

    canvas.on("mouse:wheel", (opt) => {
      const event = opt.e as WheelEvent

      let zoom = canvas.getZoom()
      zoom *= 0.999 ** event.deltaY

      if (zoom > 5) zoom = 5
      if (zoom < 0.2) zoom = 0.2

      canvas.zoomToPoint(new Point(event.offsetX, event.offsetY), zoom)

      event.preventDefault()
      event.stopPropagation()
    })

    canvas.on("mouse:down", (opt) => {
      const event = opt.e as MouseEvent

      if (event.altKey || event.shiftKey) {
        isDragging = true
        canvas.selection = false
        lastPosX = event.clientX
        lastPosY = event.clientY
        canvas.defaultCursor = "grabbing"
      }
    })

    canvas.on("mouse:move", (opt) => {
      if (!isDragging) return

      const event = opt.e as MouseEvent
      const vpt = canvas.viewportTransform
      if (!vpt) return

      vpt[4] += event.clientX - lastPosX
      vpt[5] += event.clientY - lastPosY

      canvas.requestRenderAll()

      lastPosX = event.clientX
      lastPosY = event.clientY
    })

    canvas.on("mouse:up", () => {
      isDragging = false
      canvas.selection = true
      canvas.defaultCursor = "default"
    })

    return () => {
      canvas.dispose()
      fabricRef.current = null
    }
  }, [])

  async function handleImageUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file || !fabricRef.current) return

    const url = URL.createObjectURL(file)
    const img = await FabricImage.fromURL(url)

    const canvas = fabricRef.current
    const canvasWidth = canvas.width ?? 900
    const canvasHeight = canvas.height ?? 620

    const imageWidth = img.width ?? 1
    const imageHeight = img.height ?? 1

    const scale = Math.min(
      (canvasWidth * 0.82) / imageWidth,
      (canvasHeight * 0.82) / imageHeight,
      1
    )

    img.set({
      left: canvasWidth / 2,
      top: canvasHeight / 2,
      originX: "center",
      originY: "center",
      scaleX: scale,
      scaleY: scale,
      selectable: false,
      evented: false,
      name: "base-image",
    })

    canvas.clear()
    canvas.backgroundColor = "#f4f4f4"
    canvas.setViewportTransform([1, 0, 0, 1, 0, 0])

    canvas.add(img)
    canvas.sendObjectToBack(img)
    canvas.renderAll()

    URL.revokeObjectURL(url)
  }

  function addSelectionBox() {
    const canvas = fabricRef.current
    if (!canvas) return

    const zoom = canvas.getZoom()
    const vpt = canvas.viewportTransform ?? [1, 0, 0, 1, 0, 0]

    const centerX = ((canvas.width ?? 900) / 2 - vpt[4]) / zoom
    const centerY = ((canvas.height ?? 620) / 2 - vpt[5]) / zoom

    const rect = new Rect({
      left: centerX - 140,
      top: centerY - 50,
      width: 280,
      height: 100,
      fill: "rgba(42,113,254,0.12)",
      stroke: "#2A71FE",
      strokeWidth: 2,
      strokeDashArray: [8, 6],
      selectable: true,
      hasControls: true,
      name: "repair-selection",
    })

    canvas.add(rect)
    canvas.setActiveObject(rect)
    canvas.renderAll()
  }

  async function cropSelectionToBlob(): Promise<Blob> {
    const canvas = fabricRef.current
    if (!canvas) {
      throw new Error("캔버스가 없습니다.")
    }

    const active = canvas.getActiveObject()
    if (!active) {
      throw new Error("보정할 영역 박스를 선택해주세요.")
    }

    const rect = active.getBoundingRect()

    const dataUrl = canvas.toDataURL({
      format: "png",
      left: rect.left,
      top: rect.top,
      width: rect.width,
      height: rect.height,
      multiplier: 2,
    })

    const res = await fetch(dataUrl)
    return await res.blob()
  }

  async function requestAiRepair() {
    const canvas = fabricRef.current
    if (!canvas) return

    try {
      setRepairing(true)

      const selectedObject = canvas.getActiveObject()
      if (!selectedObject) {
        alert("먼저 보정할 영역 박스를 선택해주세요.")
        return
      }

      const rect = selectedObject.getBoundingRect()
      const blob = await cropSelectionToBlob()

      const formData = new FormData()
      formData.append("image", blob, "selected-area.png")
      formData.append("text", text)

      const apiKey = localStorage.getItem("apiKey") ?? ""

      const res = await fetch(`${API_BASE_URL}/api/v1/image-fix/repair`, {
        method: "POST",
        headers: {
          "X-API-Key": apiKey,
        },
        body: formData,
      })

      const raw = await res.text()
      const data = raw ? (JSON.parse(raw) as RepairResponse) : null

      if (!res.ok || !data) {
        throw new Error(raw || "AI 보정 실패")
      }

      canvas.remove(selectedObject)

      const fixedImage = await FabricImage.fromURL(data.backgroundImageDataUrl)
      fixedImage.set({
        left: rect.left,
        top: rect.top,
        scaleX: rect.width / (fixedImage.width ?? rect.width),
        scaleY: rect.height / (fixedImage.height ?? rect.height),
        selectable: false,
        evented: false,
        name: "fixed-background-patch",
      })

      canvas.add(fixedImage)

      const textbox = new Textbox(data.textLayer.text, {
        left: rect.left + data.textLayer.x,
        top: rect.top + data.textLayer.y,
        width: Math.max(rect.width - 40, 80),
        fontSize,
        fill: color,
        fontFamily: data.textLayer.fontFamily,
        fontWeight: "700",
        selectable: true,
        editable: true,
        name: "korean-text-layer",
      })

      canvas.add(textbox)
      canvas.setActiveObject(textbox)
      canvas.renderAll()
    } catch (error) {
      console.error(error)
      alert(error instanceof Error ? error.message : "AI 보정 중 오류가 발생했습니다.")
    } finally {
      setRepairing(false)
    }
  }

  function addTextOnly() {
    const canvas = fabricRef.current
    if (!canvas) return

    const zoom = canvas.getZoom()
    const vpt = canvas.viewportTransform ?? [1, 0, 0, 1, 0, 0]

    const centerX = ((canvas.width ?? 900) / 2 - vpt[4]) / zoom
    const centerY = ((canvas.height ?? 620) / 2 - vpt[5]) / zoom

    const textbox = new Textbox(text, {
      left: centerX - 160,
      top: centerY - 30,
      width: 320,
      fontSize,
      fill: color,
      fontFamily: "Pretendard, Apple SD Gothic Neo, Malgun Gothic, sans-serif",
      fontWeight: "700",
      selectable: true,
      editable: true,
      name: "korean-text-layer",
    })

    canvas.add(textbox)
    canvas.setActiveObject(textbox)
    canvas.renderAll()
  }

  function deleteSelected() {
    const canvas = fabricRef.current
    if (!canvas) return

    const active = canvas.getActiveObject()
    if (!active) return

    canvas.remove(active)
    canvas.discardActiveObject()
    canvas.renderAll()
  }

  function resetView() {
    const canvas = fabricRef.current
    if (!canvas) return

    canvas.setViewportTransform([1, 0, 0, 1, 0, 0])
    canvas.renderAll()
  }

  function downloadPng() {
    const canvas = fabricRef.current
    if (!canvas) return

    const dataUrl = canvas.toDataURL({
      format: "png",
      quality: 1,
      multiplier: 2,
    })

    const a = document.createElement("a")
    a.href = dataUrl
    a.download = "korean-image-fixed.png"
    a.click()
  }

  return (
    <div className="min-h-screen bg-[#f7f8fa] text-[#161616]">
      <TopNav
        onNavigate={onNavigate}
        currentPage="korean-image-editor"
        onLogout={onLogout}
      />

      <main className="p-6">
        <div className="mx-auto max-w-7xl">
          <header className="mb-5">
            <h1 className="text-3xl font-bold">AI 이미지 한글 수정기</h1>
            <p className="mt-2 text-sm text-[#666]">
              깨진 한글 영역을 선택하면 AI가 배경을 복원하고, 진짜 한글 텍스트 레이어로 다시 얹습니다.
            </p>
          </header>

          <div className="grid gap-5 lg:grid-cols-[300px_1fr]">
            <aside className="rounded-2xl border border-[#e5e7eb] bg-white p-4 shadow-sm">
              <label className="mb-3 block text-sm font-semibold">
                이미지 첨부
              </label>

              <label className="mb-5 flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed border-[#cfd7e6] bg-[#f8fafc] px-4 py-8 text-center transition hover:border-[#2A71FE] hover:bg-[#f3f7ff]">
                <span className="mb-2 text-3xl">📎</span>
                <span className="text-sm font-semibold text-[#222]">
                  이미지 파일 첨부하기
                </span>
                <span className="mt-1 text-xs text-[#777]">
                  PNG, JPG, JPEG, WEBP
                </span>

                <input
                  type="file"
                  accept="image/png,image/jpeg,image/jpg,image/webp"
                  onChange={handleImageUpload}
                  className="hidden"
                />
              </label>

              <label className="mb-2 block text-sm font-semibold">
                교체할 한글 문구
              </label>

              <textarea
                value={text}
                onChange={(e) => setText(e.target.value)}
                className="mb-4 h-24 w-full resize-none rounded-xl border border-[#ddd] p-3 text-sm outline-none"
              />

              <label className="mb-2 block text-sm font-semibold">
                글자 크기: {fontSize}px
              </label>

              <input
                type="range"
                min={16}
                max={120}
                value={fontSize}
                onChange={(e) => setFontSize(Number(e.target.value))}
                className="mb-4 w-full"
              />

              <label className="mb-2 block text-sm font-semibold">
                글자 색상
              </label>

              <input
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value)}
                className="mb-5 h-10 w-full"
              />

              <div className="space-y-2">
                <button
                  onClick={addSelectionBox}
                  className="w-full rounded-xl border border-[#2A71FE] bg-white px-4 py-3 text-sm font-semibold text-[#2A71FE]"
                >
                  보정 영역 선택 박스 추가
                </button>

                <button
                  onClick={requestAiRepair}
                  disabled={repairing}
                  className="w-full rounded-xl bg-[#2A71FE] px-4 py-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {repairing ? "AI 보정 중..." : "AI로 선택 영역 보정"}
                </button>

                <button
                  onClick={addTextOnly}
                  className="w-full rounded-xl border border-[#ddd] bg-white px-4 py-3 text-sm font-semibold"
                >
                  텍스트만 추가
                </button>

                <button
                  onClick={deleteSelected}
                  className="w-full rounded-xl border border-[#ddd] bg-white px-4 py-3 text-sm font-semibold"
                >
                  선택 요소 삭제
                </button>

                <button
                  onClick={resetView}
                  className="w-full rounded-xl border border-[#ddd] bg-white px-4 py-3 text-sm font-semibold"
                >
                  화면 위치 초기화
                </button>

                <button
                  onClick={downloadPng}
                  className="w-full rounded-xl bg-[#16a34a] px-4 py-3 text-sm font-semibold text-white"
                >
                  PNG 다운로드
                </button>
              </div>

              <p className="mt-4 text-xs leading-relaxed text-[#777]">
                마우스 휠로 확대/축소, Shift 또는 Alt를 누른 채 드래그하면 화면을 이동할 수 있습니다.
              </p>
            </aside>

            <section className="flex min-h-[680px] items-center justify-center overflow-hidden rounded-2xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
              <div className="rounded-xl border border-[#e5e7eb] bg-[#f4f4f4] shadow-inner">
                <canvas ref={canvasElRef} />
              </div>
            </section>
          </div>
        </div>
      </main>
    </div>
  )
}