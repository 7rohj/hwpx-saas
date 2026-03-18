import { useEffect, useMemo, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"

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

  const stats = useMemo(
    () => [
        /*
      { label: "활성 템플릿", value: "12+" },
      { label: "문서 생성", value: "1,240+" },
      { label: "평균 생성 시간", value: "3초" },
       */
    ],
    []
  )

  return (
    <div className="relative min-h-screen overflow-hidden bg-white text-slate-900">
    
      <style>{`
        @keyframes float1 {
          0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
          50% { transform: translate3d(0, -18px, 0) scale(1.04); }
        }
        @keyframes float2 {
          0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
          50% { transform: translate3d(-12px, 20px, 0) scale(1.06); }
        }
        @keyframes float3 {
          0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
          50% { transform: translate3d(14px, -14px, 0) scale(1.03); }
        }
        @keyframes levitateCard {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-8px); }
        }
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
            <div className="grid items-center gap-14 lg:grid-cols-[1.05fr_0.95fr]">
              <div>
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
                    onClick={() => onNavigate("dashboard-loading")}
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

                {/*
                <div className="mt-12 flex flex-wrap items-center gap-3">
                  {stats.map((item) => (
                    
                    <div
                      key={item.label}
                      className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm"
                    >
                      <div className="text-xs font-medium text-[#1f69ef]">
                        {item.label}
                      </div>
                      <div className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">
                        {item.value}
                      </div>
                    </div>
                  ))}
                </div>
                */}
                
              </div>
              
              {/* 좌측 화면 코드 -> mp4 영상 삽입 */}
              <div className="relative">
                <div className="pointer-events-none absolute -inset-6 rounded-[40px] bg-[#1f69ef]/8 blur-2xl" />

                <div className="relative overflow-hidden rounded-[34px] border border-slate-200/80 bg-white/90 p-3 backdrop-blur-xl">
                  <div className="overflow-hidden rounded-[26px] bg-slate-100">
                    <video
                      className="block h-full w-full object-cover"
                      src="/videos/landing-demo.mp4"
                      autoPlay
                      muted
                      loop
                      playsInline
                      controls={false}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/*}
            <div className="mt-24 grid gap-6 md:grid-cols-3">
              <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)]">
                <div className="mb-4 inline-flex rounded-full bg-[#eef5ff] px-3 py-1 text-xs font-medium text-[#1f69ef]">
                  01
                </div>
                <h3 className="text-xl font-semibold tracking-tight text-slate-900">
                  템플릿 업로드
                </h3>
                <p className="mt-3 text-sm leading-7 text-slate-600">
                  기존 HWPX 템플릿을 올리고, 반복되는 문서 양식을 자동화의 시작점으로 만드세요.
                </p>
              </div>

              <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)]">
                <div className="mb-4 inline-flex rounded-full bg-[#f7fbef] px-3 py-1 text-xs font-medium text-[#7ea72c]">
                  02
                </div>
                <h3 className="text-xl font-semibold tracking-tight text-slate-900">
                  변수와 JSON 연결
                </h3>
                <p className="mt-3 text-sm leading-7 text-slate-600">
                  텍스트와 표 데이터를 변수에 매핑해, 문서 생성 규칙을 유연하게 구성할 수 있습니다.
                </p>
              </div>

              <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)]">
                <div className="mb-4 inline-flex rounded-full bg-[#fff9eb] px-3 py-1 text-xs font-medium text-[#b99218]">
                  03
                </div>
                <h3 className="text-xl font-semibold tracking-tight text-slate-900">
                  즉시 문서 생성
                </h3>
                <p className="mt-3 text-sm leading-7 text-slate-600">
                  반복 작업 없이 결과 문서를 생성하고, 생성 파일을 바로 확인하고 다운로드하세요.
                </p>
              </div>
            </div>
            */}

          </div>
        </section>
      </main>
    </div>
  )
}