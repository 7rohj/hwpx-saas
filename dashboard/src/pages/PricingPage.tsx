import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import { Check, CreditCard, Sparkles, BadgeCheck } from "lucide-react"

type PricingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const plans = [
  {
    name: "Free",
    price: "₩0",
    period: "/month",
    description: "가볍게 체험해보는 무료 플랜",
    features: [
      "10 크레딧 제공",
      "속도 제한",
    ],
    cta: "무료로 시작하기",
    highlighted: false,
  },
  {
    name: "Starter",
    price: "₩7,900",
    period: "/month",
    description: "가볍게 시작하는 개인 테스트용 플랜",
    features: [
      "300 크레딧 제공",
      "안정적인 생성 속도"

    ],
    cta: "현재 플랜",
    highlighted: true,
  },
  {
    name: "Pro",
    price: "₩27,900",
    period: "/month",
    description: "실사용과 반복 자동화를 위한 추천 플랜",
    features: [
      "1,200 크레딧 제공",
      "빠른 생성 속도",
    ],
    cta: "업그레이드 예정",
    highlighted: false,
  },
  {
    name: "Business",
    price: "Custom",
    period: "",
    description: "팀 단위 운영과 대량 문서 자동화를 위한 플랜",
    features: [
      "이메일로 연락부탁드립니다."
    ],
    cta: "문의 예정",
    highlighted: false,
  },
]

export default function PricingPage({ onNavigate, onLogout }: PricingPageProps) {
  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="pricing" onLogout={onLogout} />

      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <header className="mb-6 rounded-[32px] border border-[#cfe0ff] bg-white/85 p-6 backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                  <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
                  AutoHWPX
                </div>
                <h1 className="text-3xl font-bold tracking-tight text-[#162033] sm:text-4xl">
                  요금제
                </h1>
                <p className="mt-2 text-sm text-[#63708a] sm:text-base">
                  현재 및 예정 요금제를 한눈에 비교할 수 있는 공간입니다
                </p>
              </div>

              <div className="rounded-2xl border border-[#dbe7ff] bg-white px-4 py-3 text-sm shadow-sm">
                <p className="font-medium text-[#1a2540]">결제 기능 준비 중</p>
                <p className="text-[#63708a]">Starter Plan Active</p>
              </div>
            </div>
          </header>

          <section className="mb-6 grid gap-4 sm:grid-cols-3">
            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <p className="text-sm text-slate-500">현재 요금제</p>
                <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                  Starter
                </p>
              </div>
            </div>

            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <p className="text-sm text-slate-500">결제 상태</p>
                <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                  Inactive
                </p>
              </div>
            </div>

            <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
              <div className="h-1.5 bg-slate-200" />
              <div className="p-5">
                <p className="text-sm text-slate-500">업그레이드</p>
                <p className="mt-3 text-3xl font-bold tracking-tight text-[#162033]">
                  Soon
                </p>
              </div>
            </div>
          </section>

          <section className="grid gap-6 xl:grid-cols-2 xl:grid-cols-4">
            {plans.map((plan) => (
              <div
                key={plan.name}
                className={`rounded-[32px] border bg-white ${
                  plan.highlighted
                    ? "border-[#1f69ef] ring-1 ring-[#1f69ef]/10"
                    : "border-[#dbe7ff]"
                }`}
              >
                <div className="p-6">
                  <div className="flex items-center justify-between gap-3">
                    <h2 className="text-2xl font-semibold text-[#162033]">{plan.name}</h2>

                    {plan.highlighted ? (
                      <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-[#1f69ef] px-3 py-1 text-xs font-semibold text-white">
                        <Sparkles className="h-3.5 w-3.5" />
                        Recommended
                      </span>
                    ) : null}
                  </div>

                  <p className="mt-2 text-sm text-[#63708a] whitespace-nowrap">
                    {plan.description}
                  </p>

                  <div className="mt-6 flex items-end gap-1">
                    <span className="text-4xl font-bold tracking-tight text-[#162033]">
                      {plan.price}
                    </span>
                    {plan.period ? (
                      <span className="pb-1 text-[#63708a]">{plan.period}</span>
                    ) : null}
                  </div>

                  <button
                    className={`mt-6 flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-semibold transition ${
                      plan.highlighted
                        ? "bg-[#1f69ef] text-white hover:bg-[#1859ca]"
                        : "border border-[#d7e5ff] bg-white text-slate-700 hover:bg-[#f5f9ff]"
                    }`}
                  >
                    <CreditCard className="h-4 w-4" />
                    {plan.cta}
                  </button>

                  <div className="mt-6 space-y-3">
                    {plan.features.map((feature) => (
                      <div
                        key={feature}
                        className="flex items-start gap-3 text-sm text-[#4b5563]"
                      >
                        <span className="mt-0.5 inline-flex h-5 w-5 items-center justify-center rounded-full bg-[#eef4ff] text-[#1f69ef]">
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

          <section className="mt-6 rounded-[32px] border border-[#dbe7ff] bg-white">
            <div className="p-6">
              <h2 className="flex items-center gap-2 text-xl font-semibold text-[#162033]">
                <BadgeCheck className="h-5 w-5 text-[#1f69ef]" />
                Billing Notes
              </h2>

              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <div className="rounded-2xl border border-[#d7e5ff] bg-[#f7fbff] p-4 text-sm text-[#63708a]">
                  현재 페이지는 UI 우선 버전이며, 실제 결제 연동 전 상태를 표현합니다.
                </div>

                <div className="rounded-2xl border border-[#d7e5ff] bg-[#f7fbff] p-4 text-sm text-[#63708a]">
                  추후 토스페이먼츠 또는 Stripe 연결 시 이 영역에 결제 수단과 청구 내역을 붙이면 됩니다.
                </div>

                <div className="rounded-2xl border border-[#d7e5ff] bg-[#f7fbff] p-4 text-sm text-[#63708a]">
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