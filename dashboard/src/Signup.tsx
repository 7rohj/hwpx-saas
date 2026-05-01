import { useState } from "react"
import { KeyRound, Mail, Lock, UserPlus } from "lucide-react"

type SignupProps = {
  onSignupSuccess: () => void
  onGoLogin: () => void
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

const EMAIL_DOMAINS = [
  "naver.com",
  "gmail.com",
  "kakao.com",
  "daum.net",
  "hanmail.net",
]

export default function Signup({ onSignupSuccess, onGoLogin }: SignupProps) {
  const [emailLocalPart, setEmailLocalPart] = useState("")
  const [emailDomain, setEmailDomain] = useState("naver.com")
  const [password, setPassword] = useState("")
  const [passwordConfirm, setPasswordConfirm] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  const email = `${emailLocalPart.trim()}@${emailDomain}`

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault()

    const trimmedLocalPart = emailLocalPart.trim()

    if (!trimmedLocalPart) {
      setError("이메일 아이디를 입력해주세요.")
      return
    }

    if (trimmedLocalPart.includes("@")) {
      setError("이메일 아이디에는 @를 입력하지 말아주세요.")
      return
    }

    if (password !== passwordConfirm) {
      setError("비밀번호 확인이 일치하지 않습니다.")
      return
    }

    if (password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다.")
      return
    }

    try {
      setLoading(true)
      setError("")

      const response = await fetch(`${API_BASE_URL}/api/v1/auth/signup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
        }),
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        setError(data.message ?? "회원가입에 실패했습니다.")
        return
      }

      localStorage.setItem("userEmail", data.email)
      localStorage.setItem("apiKey", data.apiKey)

      onSignupSuccess()
    } catch (err) {
      console.error(err)
      setError("회원가입 중 오류가 발생했습니다.")
    } finally {
      setLoading(false)
    }
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
            <h1 className="text-3xl font-bold tracking-tight">회원가입</h1>
            <p className="mt-2 text-sm text-slate-500">
              이메일과 비밀번호를 입력해 계정을 만드세요.
            </p>
          </div>

          <form onSubmit={handleSignup} className="space-y-4">
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                이메일
              </label>

              <div className="flex items-center gap-2">
                <div className="flex min-w-0 flex-1 items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
                  <Mail className="h-4 w-4 shrink-0 text-slate-400" />
                  <input
                    type="text"
                    value={emailLocalPart}
                    onChange={(e) => setEmailLocalPart(e.target.value)}
                    placeholder="아이디"
                    className="min-w-0 w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                    required
                  />
                </div>

                <span className="shrink-0 text-sm font-semibold text-slate-400">
                  @
                </span>

                <select
                  value={emailDomain}
                  onChange={(e) => setEmailDomain(e.target.value)}
                  className="h-[50px] shrink-0 rounded-2xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none hover:bg-slate-50"
                >
                  {EMAIL_DOMAINS.map((domain) => (
                    <option key={domain} value={domain}>
                      {domain}
                    </option>
                  ))}
                </select>
              </div>

              <p className="mt-2 text-xs text-slate-400">
                가입 이메일: {emailLocalPart.trim() ? email : "-"}
              </p>
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
                  placeholder="8자 이상 입력"
                  className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                  required
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                비밀번호 확인
              </label>
              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <KeyRound className="h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  placeholder="비밀번호 다시 입력"
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
              <UserPlus className="h-4 w-4" />
              {loading ? "가입 중..." : "회원가입"}
            </button>

            <button
              type="button"
              onClick={onGoLogin}
              className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              로그인으로 돌아가기
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}