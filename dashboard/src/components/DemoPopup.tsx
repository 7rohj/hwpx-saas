import { useState, useEffect } from "react"

type Props = {
  onClose: () => void
}

export default function DemoPopup({ onClose }: Props) {
  const [dontShow, setDontShow] = useState(false)

  useEffect(() => {
    const hidden = localStorage.getItem("hideDemoPopup")
    if (hidden === "true") {
      onClose()
    }
  }, [])

  function handleClose() {
    if (dontShow) {
      localStorage.setItem("hideDemoPopup", "true")
    }
    onClose()
  }

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40">
      <div className="w-[420px] rounded-2xl bg-white p-6 shadow-xl relative">
        {/*
        닫기 버튼 
        <button
          onClick={handleClose}
          className="absolute top-3 right-3 text-gray-400 hover:text-gray-600 text-sm"
        >
          ✕
        </button>
        */}

        {/* 이미지 */}
        <div className="mb-4 flex justify-center">
          <img
            src="/demo-image.png"
            alt="demo"
            className="w-[200px] md:w-[260px]"
            />
        </div>

        {/* 텍스트 */}
        <h2 className="text-lg font-semibold text-center mb-2">
          현재 데모 버전입니다
        </h2>

        <p className="text-sm text-gray-500 text-center mb-6">
          일부 기능이 제한되어 있으며<br />
          실제 서비스와 다를 수 있습니다.
        </p>

        {/* 하단 */}
        <div className="flex items-center justify-between text-xs text-gray-500">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={dontShow}
              onChange={(e) => setDontShow(e.target.checked)}
            />
            다시 띄우지 않기
          </label>

          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  )
}