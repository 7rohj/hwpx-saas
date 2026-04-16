import { useEffect, useState } from "react"
import type { PageType } from "../App"

type DashboardLoadingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

export default function DashboardLoadingPage({
  onNavigate,
  onLogout,
}: DashboardLoadingPageProps) {
  const [dots, setDots] = useState(".")
  const [progress, setProgress] = useState(12)

  useEffect(() => {
    const dotTimer = window.setInterval(() => {
      setDots((prev) => {
        if (prev === ".") return ".."
        if (prev === "..") return "..."
        return "."
      })
    }, 450)

    const progressTimer = window.setInterval(() => {
      setProgress((prev) => {
        if (prev >= 92) return prev
        return prev + Math.random() * 9
      })
    }, 220)

    return () => {
      window.clearInterval(dotTimer)
      window.clearInterval(progressTimer)
    }
  }, [])

  return (
    <div className="min-h-screen overflow-hidden bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
      <div className="relative mx-auto flex min-h-screen max-w-7xl items-center justify-center px-4 sm:px-6 lg:px-8">
        <div className="w-full max-w-3xl rounded-[32px] border border-[#cfe0ff] bg-white/90 p-8 shadow-sm backdrop-blur sm:p-10">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
            <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
            AutoHWPX
          </div>

          <h1 className="text-3xl font-bold tracking-tight text-[#162033] sm:text-5xl">
            Dashboard 준비 중
          </h1>

          <p className="mt-4 text-base leading-7 text-[#63708a] sm:text-lg">
            템플릿, 생성 파일, 사용 기록을 불러오고 있어요{dots}
          </p>

          <div className="mt-10 rounded-[28px] border border-[#dbe7ff] bg-white p-6">
            <div className="mb-3 flex items-center justify-between text-sm text-[#63708a]">
              <span>Loading workspace</span>
              <span>{Math.min(100, Math.round(progress))}%</span>
            </div>

            <div className="h-3 overflow-hidden rounded-full bg-[#eaf2ff]">
              <div
                className="h-full rounded-full bg-[#1f69ef] transition-all duration-300"
                style={{ width: `${Math.min(100, progress)}%` }}
              />
            </div>
          </div>

          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <div className="h-3 w-20 animate-pulse rounded bg-slate-200" />
                <div className="mt-4 h-8 w-14 animate-pulse rounded bg-slate-200" />
              </div>
            </div>

            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <div className="h-3 w-24 animate-pulse rounded bg-slate-200" />
                <div className="mt-4 h-8 w-16 animate-pulse rounded bg-slate-200" />
              </div>
            </div>

            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <div className="h-3 w-16 animate-pulse rounded bg-slate-200" />
                <div className="mt-4 h-8 w-12 animate-pulse rounded bg-slate-200" />
              </div>
            </div>
          </div>

          <div className="mt-8 flex items-center justify-between">
            <button
              onClick={() => onNavigate("landing")}
              className="rounded-2xl border border-[#d7e5ff] bg-white px-5 py-2.5 text-sm font-medium text-slate-700 hover:bg-[#f5f9ff]"
            >
              홈으로
            </button>

            <button
              onClick={onLogout}
              className="rounded-2xl bg-[#1f69ef] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[#1859ca]"
            >
              로그아웃
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}