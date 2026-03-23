import { useState } from "react"
import { Shield, Mail, Lock } from "lucide-react"

type AdminLoginPageProps = {
  onAdminLoginSuccess: () => void
  onBackToLogin: () => void
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

export default function AdminLoginPage({
  onAdminLoginSuccess,
  onBackToLogin,
}: AdminLoginPageProps) {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  async function handleAdminLogin(e: React.FormEvent) {
    e.preventDefault()

    try {
      setLoading(true)
      setError("")

      const response = await fetch(`${API_BASE_URL}/api/v1/admin/auth/login`, {
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
        setError(data.message ?? "관리자 로그인에 실패했습니다.")
        return
      }

      if (!data.adminToken || !data.email) {
        setError("관리자 로그인은 성공했지만 adminToken 또는 email이 없습니다.")
        return
      }

      localStorage.setItem("adminEmail", data.email)
      localStorage.setItem("adminToken", data.adminToken)

      onAdminLoginSuccess()
    } catch (err) {
      console.error(err)
      setError("관리자 로그인 중 오류가 발생했습니다.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
      <div className="mx-auto flex min-h-screen max-w-7xl items-center justify-center px-4 py-10 sm:px-6 lg:px-8">
        <div className="w-full max-w-md rounded-[32px] border border-[#dbe7ff] bg-white p-8 shadow-sm">
          <div className="mb-6">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
              <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
              AutoHWPX Admin
            </div>

            <h1 className="text-3xl font-bold tracking-tight text-[#162033]">
              관리자 로그인
            </h1>

            <p className="mt-2 text-sm text-[#63708a]">
              관리자 전용 인증 레이어로 로그인합니다.
            </p>
          </div>

          <form onSubmit={handleAdminLogin} className="space-y-4">
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                이메일
              </label>
              <div className="flex items-center gap-3 rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3">
                <Mail className="h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@autohwpx.com"
                  className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                  required
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                비밀번호
              </label>
              <div className="flex items-center gap-3 rounded-2xl border border-[#d7e5ff] bg-white px-4 py-3">
                <Lock className="h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="관리자 비밀번호 입력"
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
              className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#1f69ef] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#1859ca] disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Shield className="h-4 w-4" />
              {loading ? "관리자 로그인 중..." : "관리자 로그인"}
            </button>

            <button
              type="button"
              onClick={onBackToLogin}
              className="w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm font-medium text-slate-700 hover:bg-[#f5f9ff]"
            >
              일반 로그인으로 돌아가기
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}