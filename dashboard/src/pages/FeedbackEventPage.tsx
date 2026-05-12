import { useMemo, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import {
  Coffee,
  Gift,
  MessageCircle,
  Send,
  Sparkles,
} from "lucide-react"

type FeedbackEventPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
  isLoggedIn?: boolean
}

type FeedbackItem = {
  id: number
  author: string
  content: string
  createdAt: string
}

const PAGE_SIZE = 20
const MAX_CONTENT_LENGTH = 500
const MAX_POST_PER_AUTHOR = 3

export default function FeedbackEventPage({
  onNavigate,
  onLogout,
  isLoggedIn = false,
}: FeedbackEventPageProps) {
  const [nickname, setNickname] = useState("")
  const [content, setContent] = useState("")
  const [done, setDone] = useState(false)

  const [feedbacks, setFeedbacks] = useState<FeedbackItem[]>([
    {
      id: 1,
      author: "문서작업러",
      content:
        "표 안의 표 스타일도 자동 적용되면 진짜 혁명일 것 같습니다.",
      createdAt: "2026.05.03 19:42",
    },
    {
      id: 2,
      author: "익명",
      content:
        "실행 전후 미리보기 비교 기능이 있으면 좋겠어요.",
      createdAt: "2026.05.03 20:10",
    },
  ])

  const pagedFeedbacks = useMemo(() => {
    return feedbacks.slice(0, PAGE_SIZE)
  }, [feedbacks])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    const trimmedNickname =
      nickname.trim() || "익명"

    const trimmedContent = content.trim()

    if (!trimmedContent) {
      alert("의견을 입력해주세요.")
      return
    }

    if (trimmedContent.length > MAX_CONTENT_LENGTH) {
      alert("댓글은 최대 500자까지 입력 가능합니다.")
      return
    }

    const authorPostCount = feedbacks.filter(
      (item) => item.author === trimmedNickname
    ).length

    if (authorPostCount >= MAX_POST_PER_AUTHOR) {
      alert("한 작성자는 최대 3개의 댓글만 작성 가능합니다.")
      return
    }

    const now = new Date()

    const formattedDate = `${now.getFullYear()}.${String(
      now.getMonth() + 1
    ).padStart(2, "0")}.${String(now.getDate()).padStart(
      2,
      "0"
    )} ${String(now.getHours()).padStart(
      2,
      "0"
    )}:${String(now.getMinutes()).padStart(2, "0")}`

    const newFeedback: FeedbackItem = {
      id: Date.now(),
      author: trimmedNickname,
      content: trimmedContent,
      createdAt: formattedDate,
    }

    setFeedbacks((prev) => [newFeedback, ...prev])

    setDone(true)
    setNickname("")
    setContent("")
  }

  return (
    <>
      <TopNav
        onNavigate={onNavigate}
        currentPage="feedback"
        onLogout={onLogout}
        isLoggedIn={isLoggedIn}
      />

      <div className="min-h-screen bg-gradient-to-br from-[#f5f9ff] via-white to-[#fff8e1] px-6 py-10 text-slate-900">

        <main className="mx-auto max-w-7xl">

          <section className="overflow-hidden bg-white">

            <div className="grid gap-0 lg:grid-cols-[1fr_0.9fr]">

              <div className="p-8 sm:p-10">

                <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-[#edf5ff] px-4 py-2 text-sm font-semibold text-[#1f69ef]">
                  <Sparkles className="h-4 w-4" />
                  AutoHWPX 의견 이벤트
                </div>

                <h1 className="text-4xl font-black tracking-tight sm:text-5xl">
                  한마디 남기고
                  <br />
                  커피 받아가세요 ☕
                </h1>

                <p className="mt-5 max-w-xl text-base leading-7 text-slate-500">
                  AutoHWPX를 사용하며 불편했던 점,
                  추가되었으면 하는 기능,
                  응원의 한마디를 남겨주세요.
                  좋은 의견은 실제 서비스 개선에 반영하고,
                  랜덤 추첨으로 스타벅스 기프티콘을 보내드립니다.
                </p>

                <div className="mt-8 grid gap-3 sm:grid-cols-3">

                  <div className="bg-slate-50 p-5">
                    <MessageCircle className="mb-3 h-6 w-6 text-[#1f69ef]" />
                    <p className="text-sm font-bold">
                      의견 작성
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      1분이면 충분해요
                    </p>
                  </div>

                  <div className="bg-slate-50 p-5">
                    <Gift className="mb-3 h-6 w-6 text-[#1f69ef]" />
                    <p className="text-sm font-bold">
                      랜덤 추첨
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      작성자 대상
                    </p>
                  </div>

                  <div className="bg-slate-50 p-5">
                    <Coffee className="mb-3 h-6 w-6 text-[#1f69ef]" />
                    <p className="text-sm font-bold">
                      스벅 기프티콘
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      감사 선물
                    </p>
                  </div>

                </div>
              </div>

              <form
                onSubmit={handleSubmit}
                className="flex flex-col justify-center bg-[#0f172a] p-8 text-white sm:p-10"
              >

                <h2 className="text-2xl font-bold">
                  의견 남기기
                </h2>

                <p className="mt-2 text-sm text-slate-300">
                  최대 500자
                </p>

                <input
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="닉네임 (비워두면 익명)"
                  className="mt-6 rounded-2xl border border-white/10 bg-white/10 px-5 py-3 text-sm text-white outline-none placeholder:text-slate-400"
                />

                <textarea
                  value={content}
                  onChange={(e) => {
                    if (e.target.value.length <= MAX_CONTENT_LENGTH) {
                      setContent(e.target.value)
                    }
                  }}
                  placeholder="예: 표 스타일 적용 결과를 미리 비교할 수 있으면 좋겠어요."
                  className="mt-4 h-48 resize-none rounded-3xl border border-white/10 bg-white/10 px-5 py-4 text-sm text-white outline-none placeholder:text-slate-400"
                />

                <div className="mt-2 text-right text-xs text-slate-400">
                  {content.length} / 500
                </div>

                {done && (
                  <div className="mt-4 rounded-2xl bg-emerald-400/15 px-4 py-3 text-sm text-emerald-200">
                    의견이 등록되었습니다 ☕
                  </div>
                )}

                <button
                  type="submit"
                  className="mt-5 inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3 text-sm font-bold text-slate-900 hover:bg-slate-100"
                >
                  <Send className="h-4 w-4" />
                  의견 보내기
                </button>

              </form>

            </div>
          </section>

          {/* 댓글 리스트 */}

          <section className="mt-8 bg-white p-8">

            <div className="mb-6 flex items-center justify-between">

              <div>
                <h2 className="text-2xl font-bold">
                  최근 의견
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  최신순 · 최대 20개 표시
                </p>
              </div>

              <div className="rounded-full bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-600">
                총 {feedbacks.length}개
              </div>

            </div>

            <div className="space-y-4">

              {pagedFeedbacks.map((item) => (
                <article
                  key={item.id}
                  className="bg-slate-50 p-5"
                >

                  <div className="mb-3 flex items-center justify-between">

                    <div className="font-bold text-slate-800">
                      {item.author}
                    </div>

                    <div className="text-xs text-slate-400">
                      {item.createdAt}
                    </div>

                  </div>

                  <p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">
                    {item.content}
                  </p>

                </article>
              ))}

            </div>

          </section>

        </main>
      </div>
    </>
  )
}