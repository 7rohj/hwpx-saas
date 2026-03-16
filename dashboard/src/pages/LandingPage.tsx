import type { PageType } from "../App"
import TopNav from "../components/TopNav"

type LandingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

export default function LandingPage({
  onNavigate,
  onLogout,
}: LandingPageProps) {
  return (
  <div className="relative min-h-screen bg-[radial-gradient(circle_at_top,_#f5f3ff,_#ffffff_45%,_#f8fafc_100%)] text-slate-900">
    <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(168,85,247,0.18),transparent_28%),radial-gradient(circle_at_70%_25%,rgba(59,130,246,0.16),transparent_28%),radial-gradient(circle_at_50%_80%,rgba(236,72,153,0.12),transparent_26%)]" />

    <TopNav
      onNavigate={onNavigate}
      currentPage="landing"
      onLogout={onLogout}
    />

    <main className="relative">
      <section className="relative">
          <div className="relative mx-auto max-w-7xl px-4 pb-24 pt-20 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-4xl text-center">
              <div className="mb-6 inline-flex items-center rounded-full border border-violet-200 bg-white/80 px-4 py-2 text-sm text-violet-700 shadow-sm">
                AI-powered document automation for HWPX workflows
              </div>

              <h1 className="text-5xl font-semibold leading-tight tracking-tight text-slate-950 sm:text-6xl lg:text-7xl">
                Generate polished
                <span className="block bg-gradient-to-r from-violet-600 via-fuchsia-500 to-blue-500 bg-clip-text text-transparent">
                  HWPX documents
                </span>
                from templates and JSON
              </h1>

              <p className="mx-auto mt-6 max-w-2xl text-lg leading-8 text-slate-600">
                Upload templates, inspect variables, send structured JSON, and generate
                production-ready documents in seconds.
              </p>

              <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
                <button
                  onClick={() => onNavigate("dashboard-loading")}
                  className="rounded-full bg-slate-900 px-6 py-3 text-sm font-medium text-white shadow-lg shadow-slate-900/10 hover:bg-slate-800"
                >
                  Open Dashboard
                </button>
                <button
                  onClick={() => onNavigate("pricing")}
                  className="rounded-full border border-slate-200 bg-white px-6 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  View Pricing
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}