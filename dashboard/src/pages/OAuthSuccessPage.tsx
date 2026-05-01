import { useEffect } from "react"
import type { PageType } from "../App"

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"

type OAuthSuccessPageProps = {
  onNavigate: (page: PageType) => void
  onLoginSuccess: () => void
}

export default function OAuthSuccessPage({
  onNavigate,
  onLoginSuccess,
}: OAuthSuccessPageProps) {
  useEffect(() => {
    async function loadSession() {
      const params = new URLSearchParams(window.location.search)
      const error = params.get("error")

      if (error) {
        window.history.replaceState({}, "", "/")
        onNavigate("landing")
        return
      }

      try {
        const res = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
          method: "GET",
          credentials: "include",
        })

        const data = await res.json()

        if (!res.ok || !data.ok || !data.email) {
          window.history.replaceState({}, "", "/")
          onNavigate("landing")
          return
        }

        localStorage.setItem("userEmail", data.email)
        localStorage.setItem("loginType", "oauth")

        if (data.lastLoginAt) {
          localStorage.setItem("lastLoginAt", data.lastLoginAt)
        }

        // 핵심: OAuth 처리 끝나면 주소를 랜딩으로 바꿈
        window.history.replaceState({}, "", "/")

        onLoginSuccess()
      } catch (err) {
        console.error(err)
        window.history.replaceState({}, "", "/")
        onNavigate("landing")
      }
    }

    loadSession()
  }, [onNavigate, onLoginSuccess])

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 text-slate-900">
      <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <p className="text-lg font-bold">소셜 로그인 처리 중...</p>
        <p className="mt-2 text-sm text-slate-500">
          잠시 후 AutoHWPX로 이동합니다.
        </p>
      </div>
    </div>
  )
}