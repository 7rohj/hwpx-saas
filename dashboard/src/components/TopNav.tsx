import type { PageType } from "../App"

type TopNavProps = {
  onNavigate: (page: PageType) => void
  currentPage: PageType
  onLogout?: () => void
}

export default function TopNav({
  onNavigate,
  currentPage,
  onLogout,
}: TopNavProps) {
  const menu = [
    ["홈화면", "landing"],
    ["대시보드", "dashboard"],
    ["API Keys", "apikeys"],
    ["사용량 로그", "usage"],
    ["요금운영", "pricing"],
  ] as const

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/80 backdrop-blur-xl">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <button onClick={() => onNavigate("landing")} className="text-left">
          <div className="text-lg font-semibold tracking-tight text-slate-900">
            AutoHWPX
          </div>
          <div className="text-xs text-slate-500">Document Automation SaaS</div>
        </button>

        <nav className="hidden items-center gap-2 md:flex">
          {menu.map(([label, page]) => {
            const active = currentPage === page
            return (
              <button
                key={page}
                onClick={() => onNavigate(page)}
                className={`rounded-full px-4 py-2 text-sm transition ${
                  active
                    ? "bg-slate-900 text-white"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {label}
              </button>
            )
          })}
        </nav>

        <div className="flex items-center gap-2">
          {onLogout ? (
            <button
              onClick={onLogout}
              className="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              로그아웃
            </button>
          ) : null}
        </div>
      </div>
    </header>
  )
}