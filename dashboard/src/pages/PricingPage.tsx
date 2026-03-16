import type { PageType } from "../App"
import TopNav from "../components/TopNav"

type PricingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

export default function PricingPage({
  onNavigate,
  onLogout,
}: PricingPageProps) {
  return (
    <div className="min-h-screen bg-slate-50">
      <TopNav onNavigate={onNavigate} currentPage="pricing" onLogout={onLogout} />

      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="rounded-[28px] border border-slate-200 bg-white p-8 shadow-sm">
          <h1 className="text-3xl font-semibold text-slate-900">Pricing</h1>
          <p className="mt-3 text-slate-600">여기에 요금제 페이지를 만들 예정입니다.</p>
        </div>
      </div>
    </div>
  )
}