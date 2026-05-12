import { useEffect, useMemo, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import {
  Coffee,
  Gift,
  MessageCircle,
  Send,
  Sparkles,
  Pencil,
  Trash2,
  X,
  Check,
} from "lucide-react"

type FeedbackEventPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
  isLoggedIn?: boolean
}

type FeedbackItem = {
  id: number
  userEmail: string
  maskedName: string
  content: string
  createdAt: string
  updatedAt?: string | null
}

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

const MAX_CONTENT_LENGTH = 500

function formatDateTime(value: string) {
  try {
    return new Date(value).toLocaleString("ko-KR", {
      hour12: false,
    })
  } catch {
    return value
  }
}

function maskEmailForDisplay(email: string) {
  const local = email.split("@")[0]

  if (local.length <= 4) {
    return local + "****"
  }

  return local.substring(0, 4) + "****"
}

export default function FeedbackEventPage({
  onNavigate,
  onLogout,
  isLoggedIn = false,
}: FeedbackEventPageProps) {
  const [content, setContent] = useState("")
  const [done, setDone] = useState(false)
  const [loading, setLoading] = useState(false)
  const [feedbacks, setFeedbacks] = useState<FeedbackItem[]>([])
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState("")

  const currentUserEmail = localStorage.getItem("userEmail") ?? ""

  const visibleFeedbacks = useMemo(() => {
    return feedbacks.slice(0, 20)
  }, [feedbacks])

  async function loadFeedbacks() {
    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/feedback/comments`, {
        method: "GET",
      })

      if (!res.ok) {
        throw new Error("댓글 목록을 불러오지 못했습니다.")
      }

      const data = await res.json()
      setFeedbacks(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error(err)
    }
  }

  useEffect(() => {
    loadFeedbacks()
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    const trimmedContent = content.trim()

    if (!isLoggedIn || !currentUserEmail) {
      alert("로그인 후 이용 부탁드려요.")
      return
    }

    if (!trimmedContent) {
      alert("의견을 입력해주세요.")
      return
    }

    if (trimmedContent.length > MAX_CONTENT_LENGTH) {
      alert("댓글은 최대 500자까지 입력 가능합니다.")
      return
    }

    try {
      setLoading(true)

      const res = await fetch(`${API_BASE_URL}/api/v1/feedback/comments`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Email": currentUserEmail,
        },
        body: JSON.stringify({
          content: trimmedContent,
        }),
      })

      const data = await res.json().catch(() => null)

      if (!res.ok) {
        throw new Error(data?.message ?? "의견 등록에 실패했습니다.")
      }

      setDone(true)
      setContent("")
      await loadFeedbacks()
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "의견 등록에 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  function startEdit(item: FeedbackItem) {
    setEditingId(item.id)
    setEditContent(item.content)
  }

  function cancelEdit() {
    setEditingId(null)
    setEditContent("")
  }

  async function handleUpdate(id: number) {
    const trimmedContent = editContent.trim()

    if (!trimmedContent) {
      alert("수정할 내용을 입력해주세요.")
      return
    }

    if (trimmedContent.length > MAX_CONTENT_LENGTH) {
      alert("댓글은 최대 500자까지 입력 가능합니다.")
      return
    }

    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/feedback/comments/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "X-User-Email": currentUserEmail,
        },
        body: JSON.stringify({
          content: trimmedContent,
        }),
      })

      const data = await res.json().catch(() => null)

      if (!res.ok) {
        throw new Error(data?.message ?? "댓글 수정에 실패했습니다.")
      }

      setEditingId(null)
      setEditContent("")
      await loadFeedbacks()
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "댓글 수정에 실패했습니다.")
    }
  }

  async function handleDelete(id: number) {
    const ok = window.confirm("댓글을 삭제하시겠습니까?")
    if (!ok) return

    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/feedback/comments/${id}`, {
        method: "DELETE",
        headers: {
          "X-User-Email": currentUserEmail,
        },
      })

      if (!res.ok) {
        const data = await res.json().catch(() => null)
        throw new Error(data?.message ?? "댓글 삭제에 실패했습니다.")
      }

      await loadFeedbacks()
    } catch (err) {
      console.error(err)
      alert(err instanceof Error ? err.message : "댓글 삭제에 실패했습니다.")
    }
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
                  의견이벤트
                </div>

                <h1 className="text-4xl font-black tracking-tight sm:text-5xl">
                  한마디 남기고
                  <br />
                  커피 받아가세요 ☕
                </h1>

                <p className="mt-5 max-w-xl text-base leading-7 text-slate-500">
                  AutoHWPX를 사용하며 불편했던 점, 추가되었으면 하는 기능,
                  응원의 한마디를 남겨주세요. 좋은 의견은 실제 서비스 개선에
                  반영하고, 랜덤 추첨으로 스타벅스 기프티콘을 보내드립니다.
                </p>

                <div className="mt-8 grid gap-3 sm:grid-cols-3">
                  <div className="bg-slate-50 p-5">
                    <MessageCircle className="mb-3 h-6 w-6 text-[#1f69ef]" />
                    <p className="text-sm font-bold">의견 작성</p>
                    <p className="mt-1 text-xs text-slate-500">
                      1분이면 충분해요
                    </p>
                  </div>

                  <div className="bg-slate-50 p-5">
                    <Gift className="mb-3 h-6 w-6 text-[#1f69ef]" />
                    <p className="text-sm font-bold">랜덤 추첨</p>
                    <p className="mt-1 text-xs text-slate-500">작성자 대상</p>
                  </div>

                  <div className="bg-slate-50 p-5">
                    <Coffee className="mb-3 h-6 w-6 text-[#1f69ef]" />
                    <p className="text-sm font-bold">스벅 기프티콘</p>
                    <p className="mt-1 text-xs text-slate-500">감사 선물</p>
                  </div>
                </div>
              </div>

              <form
                onSubmit={handleSubmit}
                className="flex flex-col justify-center bg-[#0f172a] p-8 text-white sm:p-10"
              >
                <h2 className="text-2xl font-bold">의견 남기기</h2>
                <p className="mt-2 text-sm text-slate-300">
                  최대 500자 · 작성자당 최대 3개
                </p>

                <textarea
                  value={content}
                  onChange={(e) => {
                    if (e.target.value.length <= MAX_CONTENT_LENGTH) {
                      setContent(e.target.value)
                    }
                  }}
                  placeholder={
                    isLoggedIn
                      ? "예: 표 스타일 적용 결과를 미리 비교할 수 있으면 좋겠어요."
                      : "로그인 후 의견을 남길 수 있습니다."
                  }
                  disabled={!isLoggedIn}
                  className="mt-6 h-48 resize-none rounded-3xl border border-white/10 bg-white/10 px-5 py-4 text-sm text-white outline-none placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-50"
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
                  disabled={loading || !isLoggedIn}
                  className="mt-5 inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3 text-sm font-bold text-slate-900 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Send className="h-4 w-4" />
                  {loading ? "등록 중..." : "의견 보내기"}
                </button>
              </form>
            </div>
          </section>

          <section className="mt-8 bg-white p-8">
            <div className="mb-6 flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold">최근 의견</h2>
                <p className="mt-1 text-sm text-slate-500">
                  최신순 · 한 페이지 최대 20개 표시
                </p>
              </div>

              <div className="rounded-full bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-600">
                총 {feedbacks.length}개
              </div>
            </div>

            <div className="space-y-4">
              {visibleFeedbacks.length === 0 ? (
                <div className="bg-slate-50 p-8 text-center text-sm text-slate-400">
                  아직 등록된 의견이 없습니다.
                </div>
              ) : (
                visibleFeedbacks.map((item) => {
                  const isMine = currentUserEmail && item.userEmail === currentUserEmail
                  const isEditing = editingId === item.id

                  return (
                    <article key={item.id} className="bg-slate-50 p-5">
                      <div className="mb-3 flex items-center justify-between gap-3">
                        <div>
                          <div className="flex items-center gap-2">

                            <div className="font-bold text-slate-800">
                              {item.maskedName}
                            </div>

                            <div className="rounded-full bg-slate-200 px-2 py-0.5 text-[11px] font-semibold text-slate-500">
                              {maskEmailForDisplay(item.userEmail)}
                            </div>

                            {isMine && (
                              <div className="rounded-full bg-[#1f69ef]/10 px-2 py-0.5 text-[11px] font-semibold text-[#1f69ef]">
                                내 댓글
                              </div>
                            )}

                          </div>
                          <div className="mt-1 text-xs text-slate-400">
                            작성일 {formatDateTime(item.createdAt)}
                            {item.updatedAt ? ` · 수정됨 ${formatDateTime(item.updatedAt)}` : ""}
                          </div>
                        </div>

                        {isMine && !isEditing ? (
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              onClick={() => startEdit(item)}
                              className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100"
                            >
                              <Pencil className="h-3.5 w-3.5" />
                              수정
                            </button>

                            <button
                              type="button"
                              onClick={() => handleDelete(item.id)}
                              className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-red-500 hover:bg-red-50"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                              삭제
                            </button>
                          </div>
                        ) : null}
                      </div>

                      {isEditing ? (
                        <div>
                          <textarea
                            value={editContent}
                            onChange={(e) => {
                              if (e.target.value.length <= MAX_CONTENT_LENGTH) {
                                setEditContent(e.target.value)
                              }
                            }}
                            className="h-32 w-full resize-none bg-white px-4 py-3 text-sm leading-6 text-slate-700 outline-none"
                          />

                          <div className="mt-2 flex items-center justify-between">
                            <span className="text-xs text-slate-400">
                              {editContent.length} / 500
                            </span>

                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                onClick={cancelEdit}
                                className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-slate-100"
                              >
                                <X className="h-3.5 w-3.5" />
                                취소
                              </button>

                              <button
                                type="button"
                                onClick={() => handleUpdate(item.id)}
                                className="inline-flex items-center gap-1 rounded-full bg-[#1f69ef] px-3 py-1.5 text-xs font-semibold text-white hover:bg-[#195bd0]"
                              >
                                <Check className="h-3.5 w-3.5" />
                                저장
                              </button>
                            </div>
                          </div>
                        </div>
                      ) : (
                        <p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">
                          {item.content}
                        </p>
                      )}
                    </article>
                  )
                })
              )}
            </div>
          </section>
        </main>
      </div>
    </>
  )
}