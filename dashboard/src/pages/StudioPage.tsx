import { useMemo, useState } from "react"
import type { PageType } from "../App"
import TopNav from "../components/TopNav"
import {
  ArrowRight,
  CheckCircle2,
  FileText,
  PanelLeft,
  Search,
  Upload,
  Wand2,
  FileUp,
  LayoutTemplate,
} from "lucide-react"

type StudioPageProps = {
  onNavigate: (page: PageType) => void
  onLogout: () => void
}

type RecentItemType = "generated" | "template" | "source"

type RecentItem = {
  id: string
  title: string
  type: RecentItemType
  updatedAt: string
}

const initialRecentItems: RecentItem[] = [
  {
    id: "1",
    title: "견적서_최종본_2026_03_24.hwpx",
    type: "generated",
    updatedAt: "방금 전",
  },
  {
    id: "2",
    title: "공공기관_제안서_템플릿.hwpx",
    type: "template",
    updatedAt: "15분 전",
  },
  {
    id: "3",
    title: "회사소개서_원본.hwp",
    type: "source",
    updatedAt: "1시간 전",
  },
  {
    id: "4",
    title: "업무보고서_스타일적용결과.hwpx",
    type: "generated",
    updatedAt: "어제",
  },
]

export default function StudioPage({ onNavigate, onLogout }: StudioPageProps) {
  const [prompt, setPrompt] = useState("")
  const [templateFile, setTemplateFile] = useState<File | null>(null)
  const [sourceFile, setSourceFile] = useState<File | null>(null)
  const [recentItems, setRecentItems] = useState<RecentItem[]>(initialRecentItems)
  const [isGenerating, setIsGenerating] = useState(false)
  const [selectedRecentId, setSelectedRecentId] = useState<string | null>(null)

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

  const handleRecentClick = (item: RecentItem) => {
    setSelectedRecentId(item.id)

    if (item.type === "template") {
      setPrompt("이 템플릿 형식을 기준으로 문서를 정리해줘")
    } else if (item.type === "source") {
      setPrompt("이 원본문서 내용을 보기 좋게 정리해줘")
    } else {
      setPrompt("이전 생성 결과와 유사한 형식으로 다시 만들어줘")
    }
  }

  const handleGenerate = async () => {
    if (!canGenerate) return

    try {
      setIsGenerating(true)

      // TODO: 실제 API 연동
      // const formData = new FormData()
      // formData.append("templateFile", templateFile!)
      // formData.append("sourceFile", sourceFile!)
      // formData.append("prompt", prompt)
      // await fetch("/api/v1/compose", { method: "POST", body: formData })

      await new Promise((resolve) => setTimeout(resolve, 1300))

      const generatedName =
        `${sourceFile?.name?.replace(/\.[^/.]+$/, "") ?? "문서"}_styled.hwpx`

      const newItem: RecentItem = {
        id: String(Date.now()),
        title: generatedName,
        type: "generated",
        updatedAt: "방금 전",
      }

      setRecentItems((prev) => [newItem, ...prev])
      setSelectedRecentId(newItem.id)
    } finally {
      setIsGenerating(false)
    }
  }

  return (
    <>
      <TopNav onNavigate={onNavigate} currentPage="studio" onLogout={onLogout} />

      <div className="min-h-screen bg-[linear-gradient(180deg,#f5f9ff_0%,#eef5ff_42%,#fffdf2_100%)] text-slate-900">
        <div className="flex min-h-[calc(100vh-72px)]">
          {/* 좌측 패널 */}
          <aside className="hidden w-[260px] shrink-0 bg-white xl:flex xl:flex-col">
            <div className="px-5 pt-6 pb-3">
              <p className="text-sm font-semibold text-slate-800">Recent</p>
            </div>

            <div className="px-4 pb-3">
              <div className="flex items-center gap-2 rounded-2xl bg-slate-50 px-3 py-2.5">
                <Search className="h-4 w-4 text-slate-400" />
                <input
                  className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                  placeholder="검색"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-3 pb-6">
              {recentItems.map((item) => (
                <button
                  key={item.id}
                  onClick={() => handleRecentClick(item)}
                  className={`w-full rounded-lg px-3 py-2.5 text-left transition ${
                    selectedRecentId === item.id ? "bg-slate-100" : "hover:bg-slate-50"
                  }`}
                >
                  <div className="truncate text-sm font-medium text-slate-800">
                    {item.title}
                  </div>
                  <div className="mt-0.5 text-[11px] text-slate-400">{item.updatedAt}</div>
                </button>
              ))}
            </div>
          </aside>

          {/* 메인 */}
          <main className="flex-1">
            <div className="mx-auto max-w-[1180px] px-5 py-6 md:px-8 lg:px-10">
              {/* 헤더 */}
              <header className="mb-6 flex items-center justify-between gap-4 rounded-[32px] border border-[#dbe7ff] bg-white/85 px-6 py-5 backdrop-blur">
                <div className="flex items-center gap-3">
                  <button className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-600 xl:hidden">
                    <PanelLeft className="h-5 w-5" />
                  </button>

                  <div>
                    <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-[#dbe7ff] bg-[#f4f8ff] px-3 py-1 text-xs font-semibold text-[#1f69ef]">
                      <Wand2 className="h-3.5 w-3.5" />
                      Studio
                    </div>
                    <h1 className="text-2xl font-bold tracking-tight text-[#162033] md:text-3xl">
                      템플릿 서식 자동 적용
                    </h1>
                    <p className="mt-1 text-sm text-slate-500">
                      템플릿은 형식 기준으로, 원본문서는 내용 기준으로 사용합니다.
                    </p>
                  </div>
                </div>

                <div className="hidden rounded-2xl bg-white px-4 py-3 text-sm text-slate-500 md:block">
                  작업 공간
                </div>
              </header>

              {/* 메인 콘텐츠 */}
              <section className="rounded-[32px] border border-slate-200 bg-white p-5 shadow-sm md:p-8">
                <div className="mx-auto max-w-4xl">
                  <div className="mb-5 text-center">
                    <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#dbe7ff] bg-[#f4f8ff] px-4 py-2 text-sm font-semibold text-[#1f69ef]">
                      <Wand2 className="h-4 w-4" />
                      서식은 템플릿에서, 내용은 문서에서
                    </div>

                    <h2 className="text-3xl font-bold tracking-tight text-[#162033] md:text-5xl">
                      어떤 문서를 만들고 싶나요?
                    </h2>

                    <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-slate-600 md:text-base">
                      참고 템플릿과 원본문서를 업로드하고, 원하는 스타일이나 수정 요청을 자연어로 입력해보세요.
                    </p>
                  </div>

                  <div className="rounded-[28px] bg-white p-4 md:p-5">
                    <textarea
                      value={prompt}
                      onChange={(e) => setPrompt(e.target.value)}
                      placeholder="예: 제목은 더 크게, 표 구조는 유지하고 전체 톤은 공공기관 보고서처럼 정리해줘"
                      className="min-h-[180px] w-full resize-none rounded-[24px] border border-slate-200 bg-white px-5 py-4 text-[15px] leading-7 text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-[#b8d2ff] focus:ring-4 focus:ring-[#1f69ef]/10"
                    />

                    <div className="mt-4 grid gap-4 md:grid-cols-2">
                      <UploadCard
                        title="1. 템플릿 업로드"
                        subtitle="서식 참고용 HWPX / HWP / DOCX"
                        file={templateFile}
                        onChange={(e) => handleFileChange(e, "template")}
                        onClear={() => clearFile("template")}
                        icon={<LayoutTemplate className="h-5 w-5" />}
                      />

                      <UploadCard
                        title="2. 원본문서 업로드"
                        subtitle="내용이 들어있는 한글 파일 / 텍스트 파일"
                        file={sourceFile}
                        onChange={(e) => handleFileChange(e, "source")}
                        onClear={() => clearFile("source")}
                        icon={<FileText className="h-5 w-5" />}
                      />
                    </div>

                    <div className="mt-5 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                      <div className="flex flex-wrap gap-2">
                        <SuggestionChip
                          text="공공기관 스타일"
                          onClick={() => setPrompt("공공기관 스타일로 정리해줘")}
                        />
                        <SuggestionChip
                          text="제목 크게"
                          onClick={() => setPrompt("제목은 더 크게 강조해줘")}
                        />
                        <SuggestionChip
                          text="표 서식 유지"
                          onClick={() => setPrompt("표 구조와 표 서식은 유지해줘")}
                        />
                        <SuggestionChip
                          text="보고서 톤"
                          onClick={() =>
                            setPrompt("전체 톤을 단정한 보고서 스타일로 맞춰줘")
                          }
                        />
                      </div>

                      <button
                        onClick={handleGenerate}
                        disabled={!canGenerate}
                        className="inline-flex items-center justify-center gap-2 rounded-2xl bg-[#1f2937] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#111827] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {isGenerating ? "생성 중..." : "서식 적용해서 생성하기"}
                        <ArrowRight className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </section>

              {/* 하단 요약 */}
              <section className="mt-6 grid gap-4 sm:grid-cols-3">
                <StatusCard
                  label="업로드된 템플릿"
                  value={templateFile ? "1개" : "없음"}
                />
                <StatusCard
                  label="업로드된 원본문서"
                  value={sourceFile ? "1개" : "없음"}
                />
                <StatusCard
                  label="최근 생성 상태"
                  value={isGenerating ? "생성 중" : "대기 중"}
                />
              </section>

              <section className="mt-6 grid gap-4 lg:grid-cols-3">
                <InfoCard
                  icon={<LayoutTemplate className="h-5 w-5 text-[#1f69ef]" />}
                  title="템플릿은 스타일 기준"
                  desc="제목 크기, 문단 간격, 정렬, 여백, 표 테두리 같은 레이아웃 기준으로 활용합니다."
                />
                <InfoCard
                  icon={<FileUp className="h-5 w-5 text-[#1f69ef]" />}
                  title="원본문서는 내용 소스"
                  desc="텍스트와 표를 추출해 템플릿 구조에 맞게 재배치하고 결과 문서로 생성하는 흐름입니다."
                />
                <InfoCard
                  icon={<CheckCircle2 className="h-5 w-5 text-[#1f69ef]" />}
                  title="Recent는 통합 기록"
                  desc="이전 생성 결과와 업로드 이력을 최근 작업 목록에서 다시 불러올 수 있습니다."
                />
              </section>
            </div>
          </main>
        </div>
      </div>
    </>
  )
}

type UploadCardProps = {
  title: string
  subtitle: string
  file: File | null
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  onClear: () => void
  icon: React.ReactNode
}

function UploadCard({
  title,
  subtitle,
  file,
  onChange,
  onClear,
  icon,
}: UploadCardProps) {
  return (
    <div className="rounded-[24px] border border-slate-200 bg-white p-4">
      <div className="mb-3">
        <div className="flex items-center gap-2">
          <span className="text-[#1f69ef]">{icon}</span>
          <p className="text-sm font-semibold text-[#162033]">{title}</p>
        </div>
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
              className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-white text-slate-500 transition hover:bg-slate-50"
            >
              ×
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function SuggestionChip({
  text,
  onClick,
}: {
  text: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-slate-50"
    >
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

function StatusCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="overflow-hidden rounded-[26px] border border-slate-200/70 bg-white">
      <div className="h-1.5 bg-[#dbe7ff]" />
      <div className="p-5">
        <p className="text-sm text-slate-500">{label}</p>
        <div className="mt-3 flex items-center gap-2">
          <CheckCircle2 className="h-5 w-5 text-[#1f69ef]" />
          <p className="text-2xl font-bold tracking-tight text-[#162033]">{value}</p>
        </div>
      </div>
    </div>
  )
}