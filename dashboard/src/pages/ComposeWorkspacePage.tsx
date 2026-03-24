import { useMemo, useState } from "react"
import {
  FileText,
  Upload,
  Sparkles,
  Clock3,
  FolderOpen,
  Search,
  ArrowRight,
  PanelLeft,
  Wand2,
  Trash2,
  FileUp,
} from "lucide-react"

type RecentItem = {
  id: string
  title: string
  type: "generated" | "template" | "source"
  updatedAt: string
}

const initialRecentItems: RecentItem[] = [
  {
    id: "1",
    title: "견적서_2026_03_24.hwpx",
    type: "generated",
    updatedAt: "방금 전",
  },
  {
    id: "2",
    title: "공공기관_보고서_템플릿.hwpx",
    type: "template",
    updatedAt: "12분 전",
  },
  {
    id: "3",
    title: "회사소개서_초안.hwp",
    type: "source",
    updatedAt: "1시간 전",
  },
  {
    id: "4",
    title: "제안서_생성결과_v2.hwpx",
    type: "generated",
    updatedAt: "어제",
  },
]

export default function ComposeWorkspacePage() {
  const [prompt, setPrompt] = useState("")
  const [templateFile, setTemplateFile] = useState<File | null>(null)
  const [sourceFile, setSourceFile] = useState<File | null>(null)
  const [recentItems, setRecentItems] = useState<RecentItem[]>(initialRecentItems)
  const [isGenerating, setIsGenerating] = useState(false)

  const canGenerate = useMemo(() => {
    return !!templateFile && !!sourceFile && !isGenerating
  }, [templateFile, sourceFile, isGenerating])

  const handleFileChange = (
    e: React.ChangeEvent<HTMLInputElement>,
    kind: "template" | "source"
  ) => {
    const file = e.target.files?.[0] || null
    if (!file) return

    if (kind === "template") {
      setTemplateFile(file)
    } else {
      setSourceFile(file)
    }
  }

  const clearFile = (kind: "template" | "source") => {
    if (kind === "template") {
      setTemplateFile(null)
    } else {
      setSourceFile(null)
    }
  }

  const handleGenerate = async () => {
    if (!canGenerate) return

    try {
      setIsGenerating(true)

      // TODO:
      // 여기서 실제 API 연동
      // formData.append("template", templateFile)
      // formData.append("source", sourceFile)
      // formData.append("prompt", prompt)

      await new Promise((resolve) => setTimeout(resolve, 1200))

      const newItem: RecentItem = {
        id: String(Date.now()),
        title: `${sourceFile?.name?.replace(/\.[^/.]+$/, "") ?? "새문서"}_styled.hwpx`,
        type: "generated",
        updatedAt: "방금 전",
      }

      setRecentItems((prev) => [newItem, ...prev])
    } finally {
      setIsGenerating(false)
    }
  }

  const getTypeLabel = (type: RecentItem["type"]) => {
    if (type === "generated") return "생성문서"
    if (type === "template") return "템플릿"
    return "원본문서"
  }

  const getTypeStyle = (type: RecentItem["type"]) => {
    if (type === "generated") {
      return "bg-blue-50 text-blue-700 border-blue-100"
    }
    if (type === "template") {
      return "bg-violet-50 text-violet-700 border-violet-100"
    }
    return "bg-amber-50 text-amber-700 border-amber-100"
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f7f9fc_0%,#f5f7fb_20%,#fff6eb_100%)] text-slate-900">
      <div className="flex min-h-screen">
        {/* 좌측 패널 */}
        <aside className="hidden w-[320px] shrink-0 border-r border-slate-200/80 bg-white/90 backdrop-blur xl:flex xl:flex-col">
          <div className="border-b border-slate-200/80 px-5 py-5">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#1f69ef] text-white shadow-sm">
                <Sparkles className="h-5 w-5" />
              </div>
              <div>
                <p className="text-lg font-bold tracking-tight text-[#162033]">AutoHWPX</p>
                <p className="text-sm text-slate-500">문서 생성 워크스페이스</p>
              </div>
            </div>
          </div>

          <div className="px-5 pt-5">
            <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
              <Search className="h-4 w-4 text-slate-400" />
              <input
                className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                placeholder="Recent 작업 검색"
              />
            </div>
          </div>

          <div className="mt-5 flex items-center justify-between px-5">
            <div className="flex items-center gap-2">
              <Clock3 className="h-4 w-4 text-[#1f69ef]" />
              <h2 className="text-sm font-semibold text-[#162033]">Recent 작업물</h2>
            </div>
            <button className="text-xs font-medium text-slate-500 hover:text-slate-700">
              전체보기
            </button>
          </div>

          <div className="mt-3 flex-1 overflow-y-auto px-4 pb-5">
            <div className="space-y-2">
              {recentItems.map((item) => (
                <button
                  key={item.id}
                  className="w-full rounded-2xl border border-slate-200 bg-white p-4 text-left transition hover:border-[#cfe0ff] hover:bg-[#f8fbff] hover:shadow-sm"
                >
                  <div className="mb-2 flex items-center justify-between gap-2">
                    <span
                      className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${getTypeStyle(
                        item.type
                      )}`}
                    >
                      {getTypeLabel(item.type)}
                    </span>
                    <span className="text-xs text-slate-400">{item.updatedAt}</span>
                  </div>

                  <div className="line-clamp-2 text-sm font-medium leading-5 text-[#162033]">
                    {item.title}
                  </div>
                </button>
              ))}
            </div>
          </div>

          <div className="border-t border-slate-200/80 p-4">
            <div className="rounded-3xl border border-[#dbe7ff] bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] p-4">
              <p className="text-sm font-semibold text-[#162033]">문서 작업을 더 빠르게</p>
              <p className="mt-1 text-xs leading-5 text-slate-500">
                템플릿과 원본문서를 올리고 자연어로 요청하면 바로 결과물을 생성할 수 있어요.
              </p>
            </div>
          </div>
        </aside>

        {/* 메인 */}
        <main className="flex-1">
          <div className="mx-auto max-w-[1240px] px-5 py-6 md:px-8 lg:px-10">
            {/* 상단 헤더 */}
            <header className="mb-6 flex items-center justify-between gap-4 rounded-[32px] border border-[#dbe7ff] bg-white/85 px-6 py-5 backdrop-blur">
              <div className="flex items-center gap-3">
                <button className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-600 xl:hidden">
                  <PanelLeft className="h-5 w-5" />
                </button>

                <div>
                  <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-[#cfe0ff] bg-[#edf4ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                    <span className="h-2.5 w-2.5 rounded-full bg-[#ffd400]" />
                    New Document Flow
                  </div>
                  <h1 className="text-2xl font-bold tracking-tight text-[#162033] md:text-3xl">
                    템플릿 서식 자동 적용
                  </h1>
                  <p className="mt-1 text-sm text-slate-500">
                    참고 템플릿과 원본문서를 넣으면, 스타일이 적용된 새 문서를 생성합니다.
                  </p>
                </div>
              </div>

              <div className="hidden rounded-2xl border border-[#dbe7ff] bg-white px-4 py-3 text-sm shadow-sm md:block">
                <p className="font-medium text-[#1a2540]">빠른 생성 모드</p>
                <p className="text-slate-500">Template + Source + Prompt</p>
              </div>
            </header>

            {/* 중앙 히어로 */}
            <section className="rounded-[40px] border border-[#f3dcc2] bg-[linear-gradient(180deg,#fff9f1_0%,#ffe7cf_100%)] p-5 shadow-[0_20px_80px_rgba(15,23,42,0.04)] md:p-8">
              <div className="mx-auto max-w-4xl">
                <div className="mb-5 text-center">
                  <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/80 bg-white/70 px-4 py-2 text-sm font-semibold text-[#1f69ef] shadow-sm">
                    <Wand2 className="h-4 w-4" />
                    서식은 템플릿에서, 내용은 문서에서
                  </div>

                  <h2 className="text-3xl font-bold tracking-tight text-[#162033] md:text-5xl">
                    어떤 문서를 만들고 싶나요?
                  </h2>
                  <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-slate-600 md:text-base">
                    템플릿 파일과 원본문서를 업로드하고, 원하는 스타일이나 수정사항을 자연어로 입력해보세요.
                  </p>
                </div>

                {/* 입력창 */}
                <div className="rounded-[32px] border border-white/80 bg-white/75 p-4 shadow-sm backdrop-blur md:p-5">
                  <textarea
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    placeholder="예: 제목은 더 크게, 표 구조는 유지하고 전체 톤은 공공기관 보고서처럼 정리해줘"
                    className="min-h-[180px] w-full resize-none rounded-[24px] border border-slate-200 bg-[#fbfcfe] px-5 py-4 text-[15px] leading-7 text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-[#b8d2ff] focus:ring-4 focus:ring-[#1f69ef]/10"
                  />

                  {/* 업로드 박스 */}
                  <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <UploadCard
                      title="1. 템플릿 업로드"
                      subtitle="서식 참고용 HWPX / HWP / DOCX"
                      file={templateFile}
                      onChange={(e) => handleFileChange(e, "template")}
                      onClear={() => clearFile("template")}
                    />

                    <UploadCard
                      title="2. 원본문서 업로드"
                      subtitle="내용이 들어있는 한글 파일 / 텍스트 파일"
                      file={sourceFile}
                      onChange={(e) => handleFileChange(e, "source")}
                      onClear={() => clearFile("source")}
                    />
                  </div>

                  {/* 하단 버튼영역 */}
                  <div className="mt-5 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div className="flex flex-wrap gap-2">
                      <SuggestionChip text="공공기관 스타일로 정리" />
                      <SuggestionChip text="제목 강조" />
                      <SuggestionChip text="표 서식 유지" />
                      <SuggestionChip text="단정한 보고서 톤" />
                    </div>

                    <button
                      onClick={handleGenerate}
                      disabled={!canGenerate}
                      className="inline-flex items-center justify-center gap-2 rounded-2xl bg-[#111827] px-5 py-3 text-sm font-semibold text-white transition hover:bg-black disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {isGenerating ? "생성 중..." : "서식 적용해서 생성하기"}
                      <ArrowRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </section>

            {/* 하단 가이드 카드 */}
            <section className="mt-6 grid gap-4 lg:grid-cols-3">
              <InfoCard
                icon={<FolderOpen className="h-5 w-5 text-[#1f69ef]" />}
                title="템플릿은 스타일 기준"
                desc="제목 크기, 문단 간격, 표 테두리, 정렬, 여백 같은 레이아웃 기준으로 사용합니다."
              />
              <InfoCard
                icon={<FileText className="h-5 w-5 text-[#1f69ef]" />}
                title="원본문서는 내용 소스"
                desc="텍스트와 표 데이터를 추출해서 템플릿 구조에 맞게 재배치하는 흐름으로 연결하면 됩니다."
              />
              <InfoCard
                icon={<FileUp className="h-5 w-5 text-[#1f69ef]" />}
                title="Recent는 좌측 누적"
                desc="이전 생성 결과, 업로드 템플릿, 원본문서를 좌측 패널에 쌓아두고 다시 불러오는 구조가 좋아요."
              />
            </section>
          </div>
        </main>
      </div>
    </div>
  )
}

type UploadCardProps = {
  title: string
  subtitle: string
  file: File | null
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  onClear: () => void
}

function UploadCard({ title, subtitle, file, onChange, onClear }: UploadCardProps) {
  return (
    <div className="rounded-[24px] border border-slate-200 bg-[#fbfcfe] p-4">
      <div className="mb-3">
        <p className="text-sm font-semibold text-[#162033]">{title}</p>
        <p className="mt-1 text-xs text-slate-500">{subtitle}</p>
      </div>

      {!file ? (
        <label className="flex min-h-[120px] cursor-pointer flex-col items-center justify-center rounded-[20px] border border-dashed border-[#c7d8f9] bg-white text-center transition hover:bg-[#f8fbff]">
          <Upload className="mb-2 h-5 w-5 text-[#1f69ef]" />
          <span className="text-sm font-medium text-[#162033]">파일 업로드</span>
          <span className="mt-1 text-xs text-slate-500">클릭해서 파일 선택</span>
          <input type="file" className="hidden" onChange={onChange} />
        </label>
      ) : (
        <div className="rounded-[20px] border border-[#dbe7ff] bg-white p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[#eef4ff] text-[#1f69ef]">
                <FileText className="h-5 w-5" />
              </div>
              <div>
                <p className="max-w-[240px] break-all text-sm font-medium text-[#162033]">
                  {file.name}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  {(file.size / 1024).toFixed(1)} KB
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClear}
              className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:bg-slate-50"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function SuggestionChip({ text }: { text: string }) {
  return (
    <button className="rounded-full border border-white/80 bg-white/80 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-white">
      {text}
    </button>
  )
}

function InfoCard({
  icon,
  title,
  desc,
}: {
  icon: React.ReactNode
  title: string
  desc: string
}) {
  return (
    <div className="rounded-[28px] border border-[#dbe7ff] bg-white p-5 shadow-sm">
      <div className="mb-3 inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-[#eef4ff]">
        {icon}
      </div>
      <h3 className="text-lg font-semibold text-[#162033]">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-500">{desc}</p>
    </div>
  )
}