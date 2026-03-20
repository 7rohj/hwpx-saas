import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import { Check, CreditCard, Sparkles, BadgeCheck } from "lucide-react"

type PricingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const plans = [
  {
    name: "Starter",
    price: "₩0",
    period: "/month",
    description: "가볍게 시작하는 개인 테스트용 플랜",
    features: [
      "기본 대시보드 접근",
      "API Key 발급",
      "템플릿 업로드",
      "문서 생성 테스트",
      "기본 사용 로그 조회",
    ],
    cta: "현재 플랜",
    highlighted: false,
  },
  {
    name: "Pro",
    price: "₩9,900",
    period: "/month",
    description: "실사용과 반복 자동화를 위한 추천 플랜",
    features: [
      "Starter의 모든 기능",
      "더 많은 월간 문서 생성량",
      "고급 템플릿 관리",
      "우선 처리 및 안정적 사용",
      "추후 결제/청구 기능 연동 예정",
    ],
    cta: "업그레이드 예정",
    highlighted: true,
  },
  {
    name: "Business",
    price: "Custom",
    period: "",
    description: "팀 단위 운영과 대량 문서 자동화를 위한 플랜",
    features: [
      "팀/조직 단위 확장",
      "대용량 문서 생성",
      "별도 한도/정책 협의",
      "전용 지원",
      "커스텀 계약 가능",
    ],
    cta: "문의 예정",
    highlighted: false,
  },
]

export default function PricingPage({ onNavigate, onLogout }: PricingPageProps) {
  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="pricing" onLogout={onLogout} />

      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 rounded-[28px] border border-slate-200/70 bg-white/80 p-5 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
                  <span className="h-2 w-2 rounded-full bg-emerald-500" />
                  AutoHWPX SaaS
                </div>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Pricing</h1>
                <p className="mt-2 text-sm text-slate-500 sm:text-base">
                  현재/예정 요금제를 한눈에 비교하는 공간입니다
                </p>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-sm">
                <p className="font-medium">Billing 준비 중</p>
                <p className="text-slate-500">Starter Plan Active</p>
              </div>
            </div>
          </header>

          <section className="mb-6 grid gap-4 sm:grid-cols-3">
            <div className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
              <div className="p-5">
                <p className="text-sm text-slate-500">현재 요금제</p>
                <p className="mt-3 text-3xl font-bold tracking-tight">Starter</p>
              </div>
            </div>

            <div className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
              <div className="p-5">
                <p className="text-sm text-slate-500">결제상태</p>
                <p className="mt-3 text-3xl font-bold tracking-tight">Inactive</p>
              </div>
            </div>

            <div className="rounded-[26px] border border-slate-200/70 bg-white shadow-sm">
              <div className="p-5">
                <p className="text-sm text-slate-500">업그레이드</p>
                <p className="mt-3 text-3xl font-bold tracking-tight">Soon</p>
              </div>
            </div>
          </section>

          <section className="grid gap-6 xl:grid-cols-3">
            {plans.map((plan) => (
              <div
                key={plan.name}
                className={`rounded-[30px] border bg-white shadow-sm ${
                  plan.highlighted
                    ? "border-slate-900 ring-1 ring-slate-900/10"
                    : "border-slate-200/70"
                }`}
              >
                <div className="p-6">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h2 className="text-2xl font-semibold">{plan.name}</h2>
                      <p className="mt-2 text-sm text-slate-500">{plan.description}</p>
                    </div>

                    {plan.highlighted ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-slate-900 px-3 py-1 text-xs font-semibold text-white">
                        <Sparkles className="h-3.5 w-3.5" />
                        Recommended
                      </span>
                    ) : null}
                  </div>

                  <div className="mt-6 flex items-end gap-1">
                    <span className="text-4xl font-bold tracking-tight">{plan.price}</span>
                    {plan.period ? <span className="pb-1 text-slate-500">{plan.period}</span> : null}
                  </div>

                  <button
                    className={`mt-6 flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-medium transition ${
                      plan.highlighted
                        ? "bg-slate-900 text-white hover:bg-slate-800"
                        : "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    <CreditCard className="h-4 w-4" />
                    {plan.cta}
                  </button>

                  <div className="mt-6 space-y-3">
                    {plan.features.map((feature) => (
                      <div key={feature} className="flex items-start gap-3 text-sm text-slate-600">
                        <span className="mt-0.5 inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-50 text-emerald-700">
                          <Check className="h-3.5 w-3.5" />
                        </span>
                        <span>{feature}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </section>

          <section className="mt-6 rounded-[28px] border border-slate-200/70 bg-white shadow-sm">
            <div className="p-6">
              <h2 className="flex items-center gap-2 text-xl font-semibold">
                <BadgeCheck className="h-5 w-5" />
                Billing Notes
              </h2>
              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                  현재 페이지는 UI 우선 버전이라 실제 결제 연동 전 상태를 표현합니다.
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                  추후 토스페이먼츠 또는 Stripe 연결 시 이 영역에 결제 수단/청구 내역을 붙이면 됩니다.
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                  플랜별 생성량, 팀 기능, 저장 용량 등은 이후 백엔드 정책에 맞춰 세부화하면 됩니다.
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </>
  )
}