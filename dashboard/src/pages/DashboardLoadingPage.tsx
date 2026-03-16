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
    <div className="min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,_#f5f3ff,_#ffffff_40%,_#f8fafc_100%)] text-slate-900">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(168,85,247,0.18),transparent_28%),radial-gradient(circle_at_80%_25%,rgba(59,130,246,0.16),transparent_26%),radial-gradient(circle_at_50%_80%,rgba(236,72,153,0.10),transparent_25%)]" />

      <div className="relative mx-auto flex min-h-screen max-w-7xl items-center justify-center px-4 sm:px-6 lg:px-8">
        <div className="w-full max-w-2xl rounded-[36px] border border-white/70 bg-white/80 p-10 shadow-[0_30px_80px_rgba(15,23,42,0.10)] backdrop-blur-xl">
          <div className="mb-6 inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-4 py-2 text-sm font-medium text-violet-700">
            AutoHWPX Workspace
          </div>

          <h1 className="text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
            Dashboard 준비 중
          </h1>

          <p className="mt-4 text-lg leading-8 text-slate-600">
            템플릿, 생성 파일, 사용 기록을 불러오고 있어요{dots}
          </p>

          <div className="mt-10">
            <div className="mb-3 flex items-center justify-between text-sm text-slate-500">
              <span>Loading workspace</span>
              <span>{Math.min(100, Math.round(progress))}%</span>
            </div>

            <div className="h-3 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-gradient-to-r from-violet-600 via-fuchsia-500 to-blue-500 transition-all duration-300"
                style={{ width: `${Math.min(100, progress)}%` }}
              />
            </div>
          </div>

          <div className="mt-10 grid gap-4 sm:grid-cols-3">
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="h-3 w-20 animate-pulse rounded bg-slate-200" />
              <div className="mt-4 h-8 w-14 animate-pulse rounded bg-slate-200" />
            </div>
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="h-3 w-24 animate-pulse rounded bg-slate-200" />
              <div className="mt-4 h-8 w-16 animate-pulse rounded bg-slate-200" />
            </div>
            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="h-3 w-16 animate-pulse rounded bg-slate-200" />
              <div className="mt-4 h-8 w-12 animate-pulse rounded bg-slate-200" />
            </div>
          </div>

          <div className="mt-8 flex items-center justify-between">
            <button
              onClick={() => onNavigate("landing")}
              className="rounded-full border border-slate-200 bg-white px-5 py-2.5 text-sm text-slate-700 hover:bg-slate-50"
            >
              홈으로
            </button>

            <button
              onClick={onLogout}
              className="rounded-full border border-slate-200 bg-white px-5 py-2.5 text-sm text-slate-700 hover:bg-slate-50"
            >
              로그아웃
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}