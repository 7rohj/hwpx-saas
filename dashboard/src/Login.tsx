import { useState } from "react"
import { KeyRound, Mail, Lock } from "lucide-react"

type LoginProps = {
  onLoginSuccess: () => void
  onGoSignup: () => void
  onGoAdminLogin?: () => void
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "https://api.autohwpx.com"

function RecentBadge() {
  return (
    <div className="absolute -top-3 left-1/2 z-10 -translate-x-1/2 flex flex-col items-center">
      {/* 메인 말풍선 본체 */}
      <div className="relative rounded-full bg-white px-3 py-1 text-[11px] font-semibold text-[#1f69ef] shadow-md ring-1 ring-[#d7e5ff]">
        최근 사용
      </div>

      {/* 말풍선 꼬리 (삼각형) */}
      <div 
        className="h-2 w-2 -mt-[5px] rotate-45 bg-white ring-1 ring-[#d7e5ff]" 
        style={{ 
          // 삼각형의 위쪽 절반을 잘라내어 본체와 겹치는 선을 제거합니다.
          clipPath: 'polygon(100% 100%, 0% 100%, 100% 0%)' 
        }} 
      />
    </div>
  )
}

function mapLoginErrorMessage(message?: string) {
  if (!message) return "로그인에 실패했습니다."

  if (message === "blocked user") {
    return "차단된 계정입니다. 관리자에게 문의해주세요."
  }

  if (message === "deleted user") {
    return "삭제 처리된 계정입니다. 관리자에게 문의해주세요."
  }

  if (message === "invalid email or password") {
    return "이메일 또는 비밀번호가 올바르지 않습니다."
  }

  if (message === "email and password are required") {
    return "이메일과 비밀번호를 입력해주세요."
  }

  return message
}

export default function Login({
  onLoginSuccess,
  onGoSignup,
  onGoAdminLogin,
}: LoginProps) {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  const [lastLoginMethod] = useState(() => {
    return localStorage.getItem("lastLoginMethod")
  })

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()

    try {
      setLoading(true)
      setError("")

      const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          email,
          password,
        }),
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        setError(mapLoginErrorMessage(data.message))
        return
      }

      if (!data.email || !data.apiKey) {
        setError("로그인은 성공했지만 email 또는 apiKey가 없습니다.")
        return
      }

      localStorage.setItem("userEmail", data.email)
      localStorage.setItem("apiKey", data.apiKey)
      localStorage.setItem("lastLoginMethod", "email")

      onLoginSuccess()
    } catch (err) {
      console.error(err)
      setError("로그인 중 오류가 발생했습니다.")
    } finally {
      setLoading(false)
    }
  }

  function handleGoogleLogin() {
    localStorage.setItem("lastLoginMethod", "google")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
  }

  function handleKakaoLogin() {
    localStorage.setItem("lastLoginMethod", "kakao")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/kakao`
  }

  function handleNaverLogin() {
    localStorage.setItem("lastLoginMethod", "naver")
    window.location.href = `${API_BASE_URL}/oauth2/authorization/naver`
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 text-slate-900">
      <div className="mx-auto flex min-h-screen max-w-7xl items-center justify-center px-4 py-10 sm:px-6 lg:px-8">
        <div className="w-full max-w-md rounded-[32px] border border-slate-200/70 bg-white p-8 shadow-sm">
          <div className="mb-6">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
              <span className="h-2 w-2 rounded-full bg-emerald-500" />
              AutoHWPX
            </div>
            <h1 className="text-3xl font-bold tracking-tight">로그인</h1>
            <p className="mt-2 text-sm text-slate-500">
              이메일, 비밀번호 또는 Google 계정으로 로그인해 주세요.
            </p>

            <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-800">
              <p className="font-semibold">로그인 관련 안내</p>
              <p className="mt-1">
                최근 로그인 시스템 설정 변경 과정에서 일부 기존 가입 계정 정보가 누락되었습니다.
                이용에 불편을 드려 죄송합니다. 로그인이 되지 않는 경우 번거로우시겠지만 다시 회원가입을 진행해 주세요.
              </p>
            </div>
          </div>

          <div className="relative mb-5">
  
            {/* 말풍선 */}
            {lastLoginMethod === "google" && <RecentBadge />}

            <button
              type="button"
              onClick={handleGoogleLogin}
              className="flex w-full items-center justify-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            >
              <span className="flex h-5 w-5 items-center justify-center rounded-full bg-white text-base font-bold">
                G
              </span>
              Google로 로그인
            </button>

          </div>

          <div className="relative mb-5">
            {lastLoginMethod === "kakao" && <RecentBadge />}

            <button
              type="button"
              onClick={handleKakaoLogin}
              className="flex w-full items-center justify-center gap-3 rounded-2xl bg-[#FEE500] px-4 py-3 text-sm font-semibold text-[#191919]"
            >

            {/* 카카오 아이콘 */}
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              className="h-5 w-5"
              fill="#191919"
            >
              <path d="M12 3C6.48 3 2 6.94 2 11.5c0 2.91 1.86 5.49 4.66 7.02-.2.73-.73 2.64-.84 3.06-.14.52.19.51.4.36.17-.11 2.74-1.87 3.86-2.65.62.09 1.26.14 1.92.14 5.52 0 10-3.94 10-8.5S17.52 3 12 3z"/>
            </svg>

              카카오로 로그인
            </button>
          </div>

          <div className="relative mb-5">
            {lastLoginMethod === "naver" && <RecentBadge />}

            <button
              type="button"
              onClick={handleNaverLogin}
              className="flex w-full items-center justify-center gap-3 rounded-2xl bg-[#03C75A] px-4 py-3 text-sm font-semibold text-white"
            >
              <span className="text-base font-black">N</span>

              네이버로 로그인
            </button>
          </div>

          <div className="mb-5 flex items-center gap-3">
            <div className="h-px flex-1 bg-slate-200" />
            <span className="text-xs text-slate-400">또는</span>
            <div className="h-px flex-1 bg-slate-200" />
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                이메일
              </label>
              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <Mail className="h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="test1@autohwpx.com"
                  className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                  required
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                비밀번호
              </label>
              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <Lock className="h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="비밀번호 입력"
                  className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                  required
                />
              </div>
            </div>

            {error ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
                {error}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <KeyRound className="h-4 w-4" />
              {loading ? "로그인 중..." : "로그인"}
            </button>

            <button
              type="button"
              onClick={onGoSignup}
              className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              회원가입 하러가기
            </button>

            {onGoAdminLogin ? (
              <button
                type="button"
                onClick={onGoAdminLogin}
                className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm font-medium text-[#1f69ef] hover:bg-[#f5f9ff]"
              >
                관리자 로그인
              </button>
            ) : null}
          </form>
        </div>
      </div>
    </div>
  )
}