import type { PageType } from "../App"
import TopNav from "../components/TopNav"

type UsageLogsPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

export default function UsageLogsPage({
  onNavigate,
  onLogout,
}: UsageLogsPageProps) {
  return (
    <div className="min-h-screen bg-slate-50">
      <TopNav onNavigate={onNavigate} currentPage="usage" onLogout={onLogout} />

      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="rounded-[28px] border border-slate-200 bg-white p-8 shadow-sm">
          <h1 className="text-3xl font-semibold text-slate-900">Usage & Logs</h1>
          <p className="mt-3 text-slate-600">여기에 사용량/로그 페이지를 만들 예정입니다.</p>
        </div>
      </div>
    </div>
  )
}