import { useEffect } from "react"
import type { PageType } from "../App"

type OAuthSuccessPageProps = {
  onNavigate: (page: PageType) => void
}

export default function OAuthSuccessPage({ onNavigate }: OAuthSuccessPageProps) {
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)

    const email = params.get("email")
    const apiKey = params.get("apiKey")
    const error = params.get("error")

    if (error) {
      alert(`구글 로그인 실패: ${error}`)
      onNavigate("landing")
      return
    }

    if (!email || !apiKey) {
      alert("구글 로그인은 성공했지만 email 또는 apiKey가 없습니다.")
      onNavigate("landing")
      return
    }

    localStorage.setItem("userEmail", email)
    localStorage.setItem("apiKey", apiKey)

    onNavigate("landing")
  }, [onNavigate])

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 text-slate-900">
      <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <p className="text-lg font-bold">구글 로그인 처리 중...</p>
        <p className="mt-2 text-sm text-slate-500">
          잠시 후 AutoHWPX로 이동합니다.
        </p>
      </div>
    </div>
  )
}