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
                템플릿과 JSON만 있으면, 문서는 자동으로
              </div>

              <h1 className="text-5xl font-semibold leading-tight tracking-tight text-slate-950 sm:text-6xl lg:text-7xl">
                템플릿과 JSON으로
                <span className="block bg-gradient-to-r from-violet-600 via-fuchsia-500 to-blue-500 bg-clip-text text-transparent">
                  HWPX 문서를 자동 생성하세요
                </span>
              </h1>

              <p className="mx-auto mt-6 max-w-2xl text-lg leading-8 text-slate-600">
                템플릿을 업로드하고, 변수를 확인하고, JSON 데이터를 입력하면
                반복적인 문서 작업을 몇 초 만에 자동화할 수 있습니다.
              </p>

              <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
                <button
                  onClick={() => onNavigate("dashboard-loading")}
                  className="rounded-full bg-slate-900 px-6 py-3 text-sm font-medium text-white shadow-lg shadow-slate-900/10 hover:bg-slate-800"
                >
                  대시보드 열기
                </button>
                <button
                  onClick={() => onNavigate("pricing")}
                  className="rounded-full border border-slate-200 bg-white px-6 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  요금제 보기
                </button>
              </div>
            </div>

            <div className="mt-20">
              <div className="rounded-[32px] border border-white/70 bg-white/80 p-4 shadow-[0_30px_80px_rgba(15,23,42,0.10)] backdrop-blur-xl">
                <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
                  <aside className="rounded-[24px] border border-slate-200 bg-slate-50 p-4">
                    <div className="mb-4 text-sm font-semibold text-slate-800">
                      작업 공간
                    </div>

                    <div className="space-y-2">
                      {[
                        "빠른 실행",
                        "템플릿 목록",
                        "생성된 파일",
                        "사용 로그",
                      ].map((item) => (
                        <div
                          key={item}
                          className="rounded-2xl bg-white px-4 py-3 text-sm text-slate-600 shadow-sm"
                        >
                          {item}
                        </div>
                      ))}
                    </div>
                  </aside>

                  <div className="rounded-[24px] border border-slate-200 bg-white p-6">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <h2 className="text-3xl font-semibold tracking-tight text-slate-900">
                          모든 준비가 끝났습니다 ✨
                        </h2>
                        <p className="mt-2 text-sm text-slate-500">
                          템플릿 업로드부터 변수 확인, 문서 생성까지 한 번에 이어집니다.
                        </p>
                      </div>

                      <div className="rounded-full bg-violet-50 px-4 py-2 text-sm font-medium text-violet-700">
                        실시간 미리보기
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

                    <div className="mt-8 rounded-[28px] border border-slate-200 bg-[linear-gradient(135deg,#fdf4ff_0%,#eef2ff_100%)] p-6">
                      <div className="text-sm font-medium text-slate-500">
                        예시 요청 데이터
                      </div>

                      <pre className="mt-4 overflow-x-auto rounded-2xl bg-slate-950 p-4 text-sm text-emerald-300">
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
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}