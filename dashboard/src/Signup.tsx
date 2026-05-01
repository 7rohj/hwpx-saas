import { useState } from "react"
import { KeyRound, Mail, Lock, UserPlus, ShieldCheck } from "lucide-react"

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
  const [verificationCode, setVerificationCode] = useState("")
  const [emailVerified, setEmailVerified] = useState(false)

  const [sendingCode, setSendingCode] = useState(false)
  const [verifyingCode, setVerifyingCode] = useState(false)

  const [password, setPassword] = useState("")
  const [passwordConfirm, setPasswordConfirm] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [message, setMessage] = useState("")

  const email = `${emailLocalPart.trim()}@${emailDomain}`

  function validateEmailLocalPart() {
    const trimmedLocalPart = emailLocalPart.trim()

    if (!trimmedLocalPart) {
      setError("이메일 아이디를 입력해주세요.")
      return false
    }

    if (trimmedLocalPart.includes("@")) {
      setError("이메일 아이디에는 @를 입력하지 말아주세요.")
      return false
    }

    return true
  }

  function handleEmailChange(value: string) {
    setEmailLocalPart(value)
    setEmailVerified(false)
    setVerificationCode("")
    setMessage("")
  }

  function handleDomainChange(value: string) {
    setEmailDomain(value)
    setEmailVerified(false)
    setVerificationCode("")
    setMessage("")
  }

  async function handleSendCode() {
    if (!validateEmailLocalPart()) return

    try {
      setSendingCode(true)
      setError("")
      setMessage("")

      const response = await fetch(`${API_BASE_URL}/api/v1/auth/email-code/send`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
        }),
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        setError(data.message ?? "인증번호 발송에 실패했습니다.")
        return
      }

      setMessage("인증번호를 이메일로 발송했습니다. 5분 안에 입력해주세요.")
    } catch (err) {
      console.error(err)
      setError("인증번호 발송 중 오류가 발생했습니다.")
    } finally {
      setSendingCode(false)
    }
  }

  async function handleVerifyCode() {
    if (!validateEmailLocalPart()) return

    if (!verificationCode.trim()) {
      setError("인증번호를 입력해주세요.")
      return
    }

    try {
      setVerifyingCode(true)
      setError("")
      setMessage("")

      const response = await fetch(`${API_BASE_URL}/api/v1/auth/email-code/verify`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          code: verificationCode.trim(),
        }),
      })

      const data = await response.json()

      if (!response.ok || !data.ok) {
        setError(data.message ?? "이메일 인증에 실패했습니다.")
        return
      }

      setEmailVerified(true)
      setMessage("이메일 인증이 완료되었습니다.")
    } catch (err) {
      console.error(err)
      setError("이메일 인증 중 오류가 발생했습니다.")
    } finally {
      setVerifyingCode(false)
    }
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault()

    if (!validateEmailLocalPart()) return

    if (!emailVerified) {
      setError("이메일 인증을 먼저 완료해주세요.")
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
      setMessage("")

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
              이메일 인증 후 계정을 만들 수 있습니다.
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
                    onChange={(e) => handleEmailChange(e.target.value)}
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
                  onChange={(e) => handleDomainChange(e.target.value)}
                  className="h-[50px] shrink-0 rounded-2xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none hover:bg-slate-50"
                >
                  {EMAIL_DOMAINS.map((domain) => (
                    <option key={domain} value={domain}>
                      {domain}
                    </option>
                  ))}
                </select>
              </div>

              <div className="mt-2 flex items-center justify-between gap-2">
                <p className="text-xs text-slate-400">
                  가입 이메일: {emailLocalPart.trim() ? email : "-"}
                </p>

                {emailVerified ? (
                  <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-600">
                    <ShieldCheck className="h-3 w-3" />
                    인증 완료
                  </span>
                ) : null}
              </div>

              <button
                type="button"
                onClick={handleSendCode}
                disabled={sendingCode || emailVerified}
                className="mt-3 w-full rounded-2xl border border-[#d7e5ff] px-4 py-3 text-sm font-medium text-[#1f69ef] hover:bg-[#f5f9ff] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {sendingCode
                  ? "인증번호 발송 중..."
                  : emailVerified
                  ? "이메일 인증 완료"
                  : "인증번호 발송"}
              </button>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">
                인증번호
              </label>

              <div className="flex gap-2">
                <input
                  type="text"
                  value={verificationCode}
                  onChange={(e) => {
                    setVerificationCode(e.target.value)
                    setEmailVerified(false)
                    setMessage("")
                  }}
                  placeholder="6자리 인증번호"
                  maxLength={6}
                  className="min-w-0 flex-1 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none placeholder:text-slate-400"
                  disabled={emailVerified}
                />

                <button
                  type="button"
                  onClick={handleVerifyCode}
                  disabled={verifyingCode || emailVerified}
                  className="shrink-0 rounded-2xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {verifyingCode ? "확인 중..." : "확인"}
                </button>
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

            {message ? (
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                {message}
              </div>
            ) : null}

            {error ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
                {error}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={loading || !emailVerified}
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