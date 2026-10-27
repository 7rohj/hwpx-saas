import { useEffect, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"

type LandingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
  isLoggedIn: boolean
}

type FloatingAsset = {
  type: "circle" | "pill" | "diamond" | "cloud" | "plus"
  left: string
  top: string
  size: number
  opacity: number
  duration: number
  delay: number
  color?: "blue" | "gray"
}

const ROTATING_WORDS = [
  "서식 자동 적용",
  "시간 완전 단축",
  "표 스타일 복사 가능",
  "반복 작업 간소화",
  "퀄리티 향상",
  "AutoHWPX",
]

const FLOATING_ASSETS: FloatingAsset[] = [
  { type: "cloud", left: "5%", top: "72%", size: 120, opacity: 0.48, duration: 8, delay: 0 },
  { type: "cloud", left: "14%", top: "76%", size: 160, opacity: 0.38, duration: 9, delay: 1.2 },
  { type: "cloud", left: "78%", top: "74%", size: 170, opacity: 0.42, duration: 8.5, delay: 0.6 },
  { type: "cloud", left: "68%", top: "80%", size: 130, opacity: 0.35, duration: 7.8, delay: 1.8 },

  { type: "pill", left: "11%", top: "18%", size: 82, opacity: 0.5, duration: 6.5, delay: 0.4 },
  //{ type: "pill", left: "31%", top: "13%", size: 140, opacity: 0.55, duration: 7.2, delay: 1.1 },
  { type: "pill", left: "58%", top: "17%", size: 104, opacity: 0.42, duration: 6.8, delay: 2.2 },
  { type: "pill", left: "76%", top: "23%", size: 78, opacity: 0.38, duration: 7.8, delay: 0.9 },
  { type: "pill", left: "20%", top: "61%", size: 70, opacity: 0.35, duration: 6.2, delay: 1.5 },
  { type: "pill", left: "84%", top: "57%", size: 88, opacity: 0.36, duration: 7.1, delay: 2.4 },

  { type: "circle", left: "18%", top: "28%", size: 42, opacity: 0.36, duration: 6.4, delay: 1.0 },
  { type: "circle", left: "26%", top: "46%", size: 28, opacity: 0.28, duration: 7.4, delay: 0.3 },
  { type: "circle", left: "41%", top: "26%", size: 54, opacity: 0.32, duration: 8.2, delay: 1.7 },
  { type: "circle", left: "63%", top: "39%", size: 34, opacity: 0.3, duration: 6.9, delay: 2.0 },
  { type: "circle", left: "72%", top: "31%", size: 64, opacity: 0.24, duration: 8.6, delay: 0.8 },
  { type: "circle", left: "89%", top: "68%", size: 30, opacity: 0.31, duration: 6.7, delay: 1.4 },
  { type: "circle", left: "7%", top: "48%", size: 24, opacity: 0.28, duration: 5.9, delay: 2.5 },

  { type: "diamond", left: "9%", top: "58%", size: 24, opacity: 0.5, duration: 5.8, delay: 0.2 },
  { type: "diamond", left: "16%", top: "38%", size: 18, opacity: 0.45, duration: 6.2, delay: 1.6 },
  { type: "diamond", left: "33%", top: "70%", size: 20, opacity: 0.4, duration: 7.0, delay: 0.7 },
  { type: "diamond", left: "54%", top: "64%", size: 26, opacity: 0.36, duration: 6.6, delay: 2.2 },
  { type: "diamond", left: "82%", top: "42%", size: 42, opacity: 0.42, duration: 7.2, delay: 1.1 },
  { type: "diamond", left: "91%", top: "55%", size: 22, opacity: 0.38, duration: 6.1, delay: 1.9 },

  { type: "plus", left: "70%", top: "15%", size: 48, opacity: 0.42, duration: 5.4, delay: 0.3, color: "blue" },
  { type: "plus", left: "24%", top: "21%", size: 28, opacity: 0.28, duration: 6.4, delay: 1.3 },
  { type: "plus", left: "45%", top: "76%", size: 34, opacity: 0.28, duration: 5.8, delay: 2.1 },
  { type: "plus", left: "87%", top: "26%", size: 26, opacity: 0.22, duration: 6.8, delay: 0.9, color: "blue" },
  { type: "plus", left: "12%", top: "80%", size: 30, opacity: 0.24, duration: 7.1, delay: 1.8 },

  { type: "pill", left: "4%", top: "31%", size: 58, opacity: 0.25, duration: 6.3, delay: 2.8, color: "blue" },
  { type: "circle", left: "93%", top: "16%", size: 46, opacity: 0.18, duration: 8.2, delay: 1.2, color: "blue" },
  { type: "diamond", left: "59%", top: "24%", size: 18, opacity: 0.22, duration: 6.9, delay: 0.4, color: "blue" },
  { type: "circle", left: "37%", top: "84%", size: 36, opacity: 0.2, duration: 7.6, delay: 2.6, color: "blue" },
]

function FloatingBackground() {
  return (
    <div className="pointer-events-none absolute inset-0 z-0 overflow-hidden">
      {FLOATING_ASSETS.map((asset, index) => {
        const bg =
          asset.color === "blue"
            ? "rgba(42,113,254,0.18)"
            : "rgba(148,163,184,0.22)"

        const commonStyle = {
          left: asset.left,
          top: asset.top,
          opacity: asset.opacity,
          animation: `floatAsset ${asset.duration}s ease-in-out ${asset.delay}s infinite`,
        } as const

        if (asset.type === "plus") {
          return (
            <div
              key={index}
              className="absolute font-black leading-none"
              style={{
                ...commonStyle,
                fontSize: asset.size,
                color:
                  asset.color === "blue"
                    ? "rgba(42,113,254,0.28)"
                    : "rgba(148,163,184,0.28)",
                animation: `slowDrift ${asset.duration}s ease-in-out ${asset.delay}s infinite`,
              }}
            >
              +
            </div>
          )
        }

        if (asset.type === "cloud") {
          return (
            <div
              key={index}
              className="absolute rounded-t-[999px]"
              style={{
                ...commonStyle,
                width: asset.size * 1.8,
                height: asset.size * 0.72,
                background: bg,
              }}
            />
          )
        }

        if (asset.type === "pill") {
          return (
            <div
              key={index}
              className="absolute rounded-full"
              style={{
                ...commonStyle,
                width: asset.size * 1.8,
                height: asset.size * 0.46,
                background: bg,
              }}
            />
          )
        }

        if (asset.type === "diamond") {
          return (
            <div
              key={index}
              className="absolute rotate-45 rounded-lg"
              style={{
                ...commonStyle,
                width: asset.size,
                height: asset.size,
                background: bg,
              }}
            />
          )
        }

        return (
          <div
            key={index}
            className="absolute rounded-full"
            style={{
              ...commonStyle,
              width: asset.size,
              height: asset.size,
              background: bg,
            }}
          />
        )
      })}
    </div>
  )
}

export default function LandingPage({
  onNavigate,
  onLogout,
  isLoggedIn,
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

        @keyframes floatAsset {
          0% {
            transform: translateY(30px) translateX(0) scale(0.94);
            opacity: 0;
          }
          16% {
            opacity: var(--asset-opacity, 0.6);
          }
          50% {
            transform: translateY(-8px) translateX(10px) scale(1);
            opacity: calc(var(--asset-opacity, 0.6) * 0.75);
          }
          100% {
            transform: translateY(-42px) translateX(-8px) scale(1.03);
            opacity: 0;
          }
        }

        @keyframes slowDrift {
          0%, 100% {
            transform: translateY(0px) rotate(0deg);
          }
          50% {
            transform: translateY(-16px) rotate(6deg);
          }
        }

        @keyframes rainbowGlow {
          0% {
            background-position: 0% 50%;
            filter: brightness(1);
          }
          50% {
            background-position: 100% 50%;
            filter: brightness(1.12);
          }
          100% {
            background-position: 0% 50%;
            filter: brightness(1);
          }
        }
          
      `}</style>

      <TopNav
        onNavigate={onNavigate}
        currentPage="landing"
        onLogout={onLogout}
        isLoggedIn={isLoggedIn}
      />

      <div className="relative z-20 flex w-full items-center justify-center border-b border-[#ffd9ec] bg-white px-4 py-3 text-center text-sm font-semibold">
        <span
          onClick={() => onNavigate("feedback")}
          className="cursor-pointer bg-[linear-gradient(90deg,#ff4fa3,#ffd600,#2a71fe,#ff4fa3)] bg-[length:300%_300%] bg-clip-text text-transparent transition hover:scale-[1.01] hover:brightness-110 animate-[rainbowGlow_4s_linear_infinite]"
        >
          📢 서비스 개선 의견이나 불편했던 점 남겨주시면 랜덤 추첨을 통해 스벅 기프티콘 보내드리고 있어요 ☕
        </span>
      </div>

      <main className="relative">
        <section className="relative overflow-hidden">
          <FloatingBackground />

          <div className="mx-auto flex min-h-[calc(100vh-74px)] max-w-7xl items-center justify-center px-4 py-20 sm:px-6 lg:px-8">
            <div className="relative z-10 mx-auto flex max-w-5xl flex-col items-center text-center">
              <div className="mb-7 inline-flex items-center rounded-full border border-[#cfe0ff] bg-[#f7fbff]/90 px-5 py-2.5 text-sm font-semibold text-[#1f69ef] shadow-sm backdrop-blur">
                지긋지긋 문서 서식 복사, 이제 제게 맡기세요.
              </div>

              <h1 className="font-semibold leading-[1.08] tracking-tight text-slate-950">
                <span className="block text-3xl sm:text-4xl lg:text-5xl">
                  지겨웠던 문서 작성
                </span>

                <span className="mt-4 block min-h-[1.15em] text-5xl sm:text-6xl lg:text-7xl">
                  <span className="text-[#1f69ef]">
                    {typedWord || "\u00A0"}
                  </span>
                  <span className="ml-1 inline-block h-[0.92em] w-[3px] rounded-sm bg-[#1f69ef] align-[-0.08em] animate-[blinkCursor_0.9s_step-end_infinite]" />
                </span>
              </h1>

              <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
                <button
                  onClick={() => onNavigate("composeStudio")}
                  className="group relative overflow-hidden rounded-full bg-[#1f69ef] px-8 py-3.5 text-sm font-semibold text-white shadow-[0_18px_40px_rgba(31,105,239,0.20)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#195bd0] hover:shadow-[0_22px_50px_rgba(31,105,239,0.26)]"
                >
                  <span className="relative z-10">바로 사용하기</span>
                  <span className="pointer-events-none absolute inset-y-0 left-0 w-16 bg-white/25 blur-md animate-[softShine_2.8s_ease-in-out_infinite]" />
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="text-center text-sm text-gray-500 py-6">
        © 2026 AutoHWPX. All rights reserved.
      </footer>

    </div>
  )
}