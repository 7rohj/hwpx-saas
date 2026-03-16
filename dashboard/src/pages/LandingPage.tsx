import { useEffect, useMemo, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"

type LandingPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

const LINE1 = "템플릿과 JSON으로"
const LINE2 = "HWPX 문서를 자동 생성하세요"

export default function LandingPage({
  onNavigate,
  onLogout,
}: LandingPageProps) {
  const [typedLine1, setTypedLine1] = useState("")
  const [typedLine2, setTypedLine2] = useState("")

  useEffect(() => {
    let mounted = true
    const timerIds: number[] = []

    function setSafe(line1: string, line2: string) {
      if (!mounted) return
      setTypedLine1(line1)
      setTypedLine2(line2)
    }

    function runLoop() {
      let t = 0

      setSafe("", "")

      for (let i = 1; i <= LINE1.length; i++) {
        t += 85
        timerIds.push(
          window.setTimeout(() => {
            setSafe(LINE1.slice(0, i), "")
          }, t)
        )
      }

      t += 220

      for (let i = 1; i <= LINE2.length; i++) {
        t += 75
        timerIds.push(
          window.setTimeout(() => {
            setSafe(LINE1, LINE2.slice(0, i))
          }, t)
        )
      }

      t += 6000

      timerIds.push(
        window.setTimeout(() => {
          setSafe("", "")
        }, t)
      )

      t += 1600

      timerIds.push(
        window.setTimeout(() => {
          runLoop()
        }, t)
      )
    }

    runLoop()

    return () => {
      mounted = false
      timerIds.forEach((id) => window.clearTimeout(id))
    }
  }, [])

  const stats = useMemo(
    () => [
      { label: "활성 템플릿", value: "12+", tone: "blue" },
      { label: "문서 생성", value: "1,240+", tone: "lime" },
      { label: "평균 생성 시간", value: "3초", tone: "yellow" },
    ],
    []
  )

  return (
    <div className="relative min-h-screen overflow-hidden bg-[#fcfdff] text-slate-900">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_15%_18%,rgba(31,105,239,0.16),transparent_22%),radial-gradient(circle_at_82%_18%,rgba(79,143,255,0.10),transparent_22%),radial-gradient(circle_at_58%_78%,rgba(167,217,75,0.08),transparent_18%),linear-gradient(to_bottom,#f7faff_0%,#ffffff_42%,#f8fafc_100%)]" />

      <div className="pointer-events-none absolute -left-24 top-24 h-72 w-72 rounded-full bg-[#1f69ef]/20 blur-3xl animate-[float1_10s_ease-in-out_infinite]" />
      <div className="pointer-events-none absolute right-[-40px] top-16 h-80 w-80 rounded-full bg-[#4f8fff]/16 blur-3xl animate-[float2_12s_ease-in-out_infinite]" />
      <div className="pointer-events-none absolute bottom-10 left-1/3 h-64 w-64 rounded-full bg-[#a7d94b]/10 blur-3xl animate-[float3_14s_ease-in-out_infinite]" />

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
                <div className="mb-6 inline-flex items-center rounded-full border border-[#cfe0ff] bg-white/85 px-4 py-2 text-sm font-medium text-[#1f69ef] shadow-sm backdrop-blur">
                  템플릿과 JSON만 있으면, 문서는 자동으로
                </div>

                <div className="mx-auto mt-2 max-w-4xl">
                  <h1 className="font-semibold leading-[1.12] tracking-tight text-slate-950">
                    <span className="block min-h-[1.2em] text-2xl text-slate-950 sm:text-3xl lg:text-4xl">
                      {typedLine1 || "\u00A0"}
                    </span>

                    <span className="mt-2 block min-h-[1.2em] text-4xl sm:text-5xl lg:text-6xl">
                      <span className="bg-gradient-to-r from-[#1f69ef] via-[#2f7cf6] to-[#a7d94b] bg-clip-text text-transparent">
                        {typedLine2 || "\u00A0"}
                      </span>
                      <span className="ml-1 inline-block h-[0.9em] w-[3px] rounded-sm bg-[#1f69ef] align-[-0.08em] animate-[blinkCursor_0.9s_step-end_infinite]" />
                    </span>
                  </h1>
                </div>

                <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
                  템플릿을 업로드하고, 변수를 확인하고, JSON 데이터를 입력하면
                  반복적인 HWPX 문서 작업을 몇 초 만에 자동화할 수 있습니다.
                </p>

                <div className="mt-10 flex flex-col items-start gap-3 sm:flex-row sm:items-center">
                  <button
                    onClick={() => onNavigate("dashboard-loading")}
                    className="group relative overflow-hidden rounded-full bg-[#1f69ef] px-7 py-3.5 text-sm font-medium text-white shadow-[0_18px_40px_rgba(31,105,239,0.22)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#195bd0] hover:shadow-[0_22px_50px_rgba(31,105,239,0.28)]"
                  >
                    <span className="relative z-10">대시보드 열기</span>
                    <span className="pointer-events-none absolute inset-y-0 left-0 w-16 bg-white/25 blur-md animate-[softShine_2.8s_ease-in-out_infinite]" />
                  </button>

                  <button
                    onClick={() => onNavigate("pricing")}
                    className="group rounded-full border border-slate-200 bg-white/90 px-7 py-3.5 text-sm font-medium text-slate-700 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-[#d7e4ff] hover:bg-white hover:shadow-md"
                  >
                    요금제 보기
                  </button>
                </div>

                <div className="mt-12 flex flex-wrap items-center gap-3">
                  {stats.map((item) => (
                    <div
                      key={item.label}
                      className="rounded-2xl border border-white/80 bg-white/80 px-5 py-4 shadow-sm backdrop-blur"
                    >
                      <div
                        className={`text-xs font-medium ${
                          item.tone === "blue"
                            ? "text-[#1f69ef]"
                            : item.tone === "lime"
                            ? "text-[#7ea72c]"
                            : "text-[#b99218]"
                        }`}
                      >
                        {item.label}
                      </div>
                      <div className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">
                        {item.value}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="relative">
                <div className="pointer-events-none absolute -inset-6 rounded-[40px] bg-gradient-to-r from-[#1f69ef]/16 via-[#4f8fff]/10 to-[#a7d94b]/08 blur-2xl" />

                <div className="relative animate-[levitateCard_6s_ease-in-out_infinite] rounded-[34px] border border-white/70 bg-white/80 p-4 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
                  <div className="grid gap-4 lg:grid-cols-[220px_1fr]">
                    <aside className="rounded-[26px] border border-slate-200/80 bg-slate-50/95 p-4">
                      <div className="mb-4 text-sm font-semibold text-slate-800">
                        작업 공간
                      </div>

                      <div className="space-y-2.5">
                        {[
                          "빠른 실행",
                          "템플릿 목록",
                          "생성된 파일",
                          "사용 로그",
                        ].map((item, idx) => (
                          <div
                            key={item}
                            className={`rounded-2xl px-4 py-3 text-sm shadow-sm transition ${
                              idx === 0
                                ? "border border-[#cfe0ff] bg-white text-[#1f69ef]"
                                : "bg-white text-slate-600"
                            }`}
                          >
                            {item}
                          </div>
                        ))}
                      </div>
                    </aside>

                    <div className="rounded-[26px] border border-slate-200/80 bg-white/95 p-6">
                      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <h2 className="text-3xl font-semibold tracking-tight text-slate-900">
                            문서 자동화를 더 빠르고 아름답게
                          </h2>
                          <p className="mt-2 text-sm leading-6 text-slate-500">
                            템플릿 업로드부터 변수 확인, JSON 입력, 문서 생성까지
                            하나의 워크플로우로 연결됩니다.
                          </p>
                        </div>

                        <div className="rounded-full bg-[#eef5ff] px-4 py-2 text-sm font-medium text-[#1f69ef]">
                          실시간 워크플로우
                        </div>
                      </div>

                      <div className="mt-8 grid gap-4 md:grid-cols-3">
                        <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                          <div className="text-sm font-medium text-slate-500">
                            템플릿 수
                          </div>
                          <div className="mt-3 text-3xl font-semibold text-slate-900">
                            12
                          </div>
                        </div>

                        <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                          <div className="text-sm font-medium text-slate-500">
                            생성된 문서
                          </div>
                          <div className="mt-3 text-3xl font-semibold text-slate-900">
                            38
                          </div>
                        </div>

                        <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                          <div className="text-sm font-medium text-slate-500">
                            이번 달 사용량
                          </div>
                          <div className="mt-3 text-3xl font-semibold text-slate-900">
                            84
                          </div>
                        </div>
                      </div>

                      <div className="mt-8 rounded-[28px] border border-[#dbe7ff] bg-[linear-gradient(135deg,#eef5ff_0%,#f9fbff_70%,#fbfdea_100%)] p-6">
                        <div className="text-sm font-medium text-slate-500">
                          예시 요청 데이터
                        </div>

                        <pre className="mt-4 overflow-x-auto rounded-2xl bg-slate-950 p-4 text-sm leading-6 text-emerald-300 shadow-inner">
{`{
  "templateId": "template_001",
  "outputFilename": "output.hwpx",
  "elements": [
    {
      "placeholder": "title",
      "type": "text",
      "content": "문서 제목입니다"
    }
  ]
}`}
                        </pre>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="absolute -right-4 top-10 hidden rounded-2xl border border-white/70 bg-white/88 px-4 py-3 shadow-lg backdrop-blur md:block">
                  <div className="text-xs font-medium text-slate-500">
                    문서 생성 속도
                  </div>
                  <div className="mt-1 text-xl font-semibold text-slate-900">
                    평균 3초
                  </div>
                </div>

                <div className="absolute -bottom-4 left-8 hidden rounded-2xl border border-white/70 bg-white/88 px-4 py-3 shadow-lg backdrop-blur md:block">
                  <div className="text-xs font-medium text-slate-500">
                    자동화 성공률
                  </div>
                  <div className="mt-1 text-xl font-semibold text-slate-900">
                    99.9%
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-24 grid gap-6 md:grid-cols-3">
              <div className="rounded-[28px] border border-white/80 bg-white/78 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.07)] backdrop-blur">
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

              <div className="rounded-[28px] border border-white/80 bg-white/78 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.07)] backdrop-blur">
                <div className="mb-4 inline-flex rounded-full bg-[#f6fbe8] px-3 py-1 text-xs font-medium text-[#7ea72c]">
                  02
                </div>
                <h3 className="text-xl font-semibold tracking-tight text-slate-900">
                  변수와 JSON 연결
                </h3>
                <p className="mt-3 text-sm leading-7 text-slate-600">
                  텍스트와 표 데이터를 변수에 매핑해, 문서 생성 규칙을 유연하게 구성할 수 있습니다.
                </p>
              </div>

              <div className="rounded-[28px] border border-white/80 bg-white/78 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.07)] backdrop-blur">
                <div className="mb-4 inline-flex rounded-full bg-[#fff9e8] px-3 py-1 text-xs font-medium text-[#b99218]">
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
          </div>
        </section>
      </main>
    </div>
  )
}