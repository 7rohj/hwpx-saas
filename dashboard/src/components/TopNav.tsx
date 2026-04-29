import type { PageType } from "../App"

type TopNavProps = {
  onNavigate: (page: PageType) => void
  currentPage: PageType
  onLogout?: () => void
  isLoggedIn?: boolean
}

export default function TopNav({
  onNavigate,
  currentPage,
  onLogout,
  isLoggedIn = false,
}: TopNavProps) {
  const menu = [
    ["홈화면", "landing"],
    ["스튜디오", "composeStudio"],
    // ["AI 생성 이미지 편집", "korean-image-editor"],
    // ["대시보드", "dashboard"],
    // ["My", "apikeys"],
    // ["사용량 로그", "usage"],
    // ["요금제", "pricing"],
  ] as const

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/80 backdrop-blur-xl">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        
        {/* 로고 */}
        <button onClick={() => onNavigate("landing")} className="text-left">
          <div className="text-lg font-semibold tracking-tight text-slate-900">
            AutoHWPX
          </div>
          <div className="text-xs text-slate-500">
            Document Automation SaaS
          </div>
        </button>

        {/* 메뉴 */}
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

        {/* 우측 버튼 */}
        <div className="flex items-center gap-2">
          {isLoggedIn ? (
            <button
              onClick={onLogout}
              className="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
            >
              로그아웃
            </button>
          ) : (
            <button
              onClick={() => onNavigate("login")}
              className="rounded-full bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-800"
            >
              로그인
            </button>
          )}
        </div>

      </div>
    </header>
  )
}