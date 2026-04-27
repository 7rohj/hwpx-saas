import { useEffect, useMemo, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import DemoPopup from "../components/DemoPopup"

type LandingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const ROTATING_WORDS = [
  "AutoHWPX",
  "한글 자동화",
  "삶의 질 향상",
  "업무시간단축",
]

export default function LandingPage({
  onNavigate,
  onLogout,
}: LandingPageProps) {
  const [currentWordIndex, setCurrentWordIndex] = useState(0)
  const [typedWord, setTypedWord] = useState("")
  const [isDeleting, setIsDeleting] = useState(false)
  const [showPopup, setShowPopup] = useState(true)

  useEffect(() => {
    const hidden = localStorage.getItem("hideDemoPopup")
    if (hidden === "true") {
      setShowPopup(false)
    }
  }, [])

  useEffect(() => {
    const currentWord = ROTATING_WORDS[currentWordIndex]
    let timeoutId: number

    if (!isDeleting && typedWord.length < currentWord.length) {
      timeoutId = window.setTimeout(() => {
        setTypedWord(currentWord.slice(0, typedWord.length + 1))
      }, 85)
    } else if (!isDeleting && typedWord.length === currentWord.length) {
      timeoutId = window.setTimeout(() => {
        setIsDeleting(true)
      }, 1800)
    } else if (isDeleting && typedWord.length > 0) {
      timeoutId = window.setTimeout(() => {
        setTypedWord(currentWord.slice(0, typedWord.length - 1))
      }, 45)
    } else if (isDeleting && typedWord.length === 0) {
      timeoutId = window.setTimeout(() => {
        setIsDeleting(false)
        setCurrentWordIndex((prev) => (prev + 1) % ROTATING_WORDS.length)
      }, 350)
    }

    return () => window.clearTimeout(timeoutId)
  }, [typedWord, isDeleting, currentWordIndex])

  const stats = useMemo(() => [], [])

  function handleShowDemoPopupAgain() {
    localStorage.removeItem("hideDemoPopup")
    setShowPopup(true)
  }

  return (
    <div className="relative min-h-screen overflow-hidden bg-white text-slate-900">
      <style>{`
        @keyframes softShine {
          0% { transform: translateX(-140%) skewX(-18deg); opacity: 0; }
          20% { opacity: 0.35; }
          60% { opacity: 0.12; }
          100% { transform: translateX(220%) skewX(-18deg); opacity: 0; }
        }
        @keyframes blinkCursor {
          0%, 49% { opacity: 1; }
          50%, 100% { opacity: 0; }
        }
      `}</style>

      <TopNav
        onNavigate={onNavigate}
        currentPage="landing"
        onLogout={onLogout}
      />

      <main className="relative">
        <section className="relative">
          <div className="mx-auto max-w-7xl px-4 pb-24 pt-16 sm:px-6 lg:px-8">
            <div className="relative min-h-[720px]">
              <div className="group absolute right-[-100px] top-[38%] z-0 w-[820px] -translate-y-1/2 xl:w-[980px]">
                <div className="rounded-[34px] bg-transparent transition-all duration-500 ease-out hover:z-20 hover:scale-[1.5]">
                  <video
                    className="block h-full w-full rounded-[34px] object-cover"
                    src="/videos/landing-demo.mp4"
                    autoPlay
                    muted
                    loop
                    playsInline
                    controls={false}
                  />
                </div>
              </div>

              <div className="relative z-10 max-w-3xl pt-10">
                <div className="mb-6 inline-flex items-center rounded-full border border-[#cfe0ff] bg-[#f7fbff] px-4 py-2 text-sm font-medium text-[#1f69ef] shadow-sm">
                  템플릿과 JSON만 있으면, 문서는 자동으로
                </div>

                <div className="mx-auto mt-2 max-w-4xl">
                  <h1 className="font-semibold leading-[1.08] tracking-tight text-slate-950">
                    <span className="block text-2xl sm:text-3xl lg:text-4xl">
                      작업시간을 3시간에서 10초로
                    </span>

                    <span className="mt-3 block min-h-[1.15em] text-5xl sm:text-6xl lg:text-7xl">
                      <span className="text-[#1f69ef]">
                        {typedWord || "\u00A0"}
                      </span>
                      <span className="ml-1 inline-block h-[0.92em] w-[3px] rounded-sm bg-[#1f69ef] align-[-0.08em] animate-[blinkCursor_0.9s_step-end_infinite]" />
                    </span>
                  </h1>
                </div>

                <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
                  1. 템플릿을 업로드하고,
                </p>
                <div className="max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
                  2. JSON 데이터를 입력하면
                </div>
                <div className="mt-3 max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
                  HWPX 문서 작업을 몇 초 만에 자동화할 수 있습니다.
                </div>

                <div className="mt-10 flex flex-col items-start gap-3 sm:flex-row sm:items-center">
                  <button
                    onClick={() => onNavigate("composeStudio")}
                    className="group relative overflow-hidden rounded-full bg-[#1f69ef] px-7 py-3.5 text-sm font-medium text-white shadow-[0_18px_40px_rgba(31,105,239,0.20)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#195bd0] hover:shadow-[0_22px_50px_rgba(31,105,239,0.26)]"
                  >
                    <span className="relative z-10">대시보드 열기</span>
                    <span className="pointer-events-none absolute inset-y-0 left-0 w-16 bg-white/25 blur-md animate-[softShine_2.8s_ease-in-out_infinite]" />
                  </button>

                  <button
                    onClick={() => onNavigate("pricing")}
                    className="group rounded-full border border-slate-200 bg-white px-7 py-3.5 text-sm font-medium text-slate-700 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-[#d7e4ff] hover:bg-white hover:shadow-md"
                  >
                    요금제 보기
                  </button>
                </div>
              </div>
            </div>

            {stats.map(() => null)}
          </div>
        </section>
      </main>

      <button
        type="button"
        onClick={handleShowDemoPopupAgain}
        className="fixed bottom-5 right-5 z-50 rounded-full border border-slate-200 bg-white px-4 py-2 text-xs font-medium text-slate-500 shadow-sm transition hover:text-slate-700 hover:shadow-md"
      >
        팝업 안내 다시 보기
      </button>

      {showPopup && (
        <DemoPopup onClose={() => setShowPopup(false)} />
      )}
    </div>
  )
}