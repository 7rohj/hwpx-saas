import { useEffect, useState } from "react"
import Lottie from "lottie-react"
import animaBot from "../assets/lottie/anima-bot.json"

function shimmerStyle(delay = "0s") {
  return {
    background:
      "linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)",
    backgroundSize: "200% 100%",
    animation: "previewShimmer 1.6s ease-in-out infinite",
    animationDelay: delay,
  } as const
}

const loadingMessages = [
  "파일을 불러오는 중입니다.",
  "잠시만 기다려주세요.",
  "거의 다 불러왔어요!",
]

export default function PreviewLoadingSkeletonWithBot() {
  const [messageIndex, setMessageIndex] = useState(0)

  useEffect(() => {
    const interval = window.setInterval(() => {
      setMessageIndex((prev) => (prev + 1) % loadingMessages.length)
    }, 1700)

    return () => window.clearInterval(interval)
  }, [])

  const lines = [
    { width: "72%" },
    { width: "88%" },
    { width: "81%" },
    { width: "90%" },
    { width: "64%" },
    { width: "86%" },
    { width: "78%" },
    { width: "92%" },
    { width: "69%" },
    { width: "84%" },
    { width: "74%" },
    { width: "89%" },
  ]

  return (
    <>
      <style>
        {`
          @keyframes previewShimmer {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
          }

          @keyframes botFloat {
            0% { transform: translateY(0px); }
            50% { transform: translateY(-10px); }
            100% { transform: translateY(0px); }
          }

          @keyframes loadingTextFade {
            0% {
              opacity: 0;
              transform: translateY(8px) scale(0.98);
            }
            15% {
              opacity: 1;
              transform: translateY(0px) scale(1);
            }
            75% {
              opacity: 1;
              transform: translateY(0px) scale(1);
            }
            100% {
              opacity: 0;
              transform: translateY(-6px) scale(1.02);
            }
          }
        `}
      </style>

      <div className="absolute inset-0 z-20 flex items-start justify-center">
        <div className="relative mt-6 h-[640px] w-[480px] overflow-hidden rounded-[8px] border border-[#e5e7eb] bg-white shadow-[0_4px_12px_rgba(0,0,0,0.08)]">
          {/* 기존 줄글/표 스켈레톤 영역 */}
          <div className="px-8 pt-8">
            <div
              className="mb-6 h-6 rounded"
              style={{
                width: "46%",
                ...shimmerStyle("0s"),
              }}
            />

            <div className="space-y-3">
              {lines.map((line, idx) => (
                <div
                  key={idx}
                  className="h-3 rounded"
                  style={{
                    width: line.width,
                    ...shimmerStyle(`${idx * 0.05}s`),
                  }}
                />
              ))}
            </div>

            <div className="mt-8 rounded border border-[#dbe3f0] p-4">
              <div className="mb-3 flex gap-2">
                <div
                  className="h-8 rounded"
                  style={{
                    width: "28%",
                    background:
                      "linear-gradient(90deg, #dfe6ef 25%, #f3f4f6 50%, #dfe6ef 75%)",
                    backgroundSize: "200% 100%",
                    animation: "previewShimmer 1.6s ease-in-out infinite",
                  }}
                />
                <div
                  className="h-8 rounded"
                  style={{
                    width: "32%",
                    ...shimmerStyle("0.08s"),
                  }}
                />
              </div>

              {[...Array(5)].map((_, rowIdx) => (
                <div key={rowIdx} className="mb-2 flex gap-2">
                  <div
                    className="h-7 rounded"
                    style={{
                      width: "24%",
                      ...shimmerStyle(`${rowIdx * 0.06}s`),
                    }}
                  />
                  <div
                    className="h-7 rounded"
                    style={{
                      width: "74%",
                      background:
                        "linear-gradient(90deg, #eef2f7 25%, #f8fafc 50%, #eef2f7 75%)",
                      backgroundSize: "200% 100%",
                      animation: "previewShimmer 1.6s ease-in-out infinite",
                      animationDelay: `${rowIdx * 0.08}s`,
                    }}
                  />
                </div>
              ))}
            </div>
          </div>

          {/* 중앙 봇 + 멘트 오버레이 */}
            <div className="pointer-events-none absolute inset-0 z-30 flex items-center justify-center">
            <div className="flex flex-col items-center">
                <div className="relative h-[150px] w-[150px]">
                <div
                    className="relative h-full w-full"
                    style={{
                    filter: `
                        drop-shadow(2px 0 0 #ffffff)
                        drop-shadow(-2px 0 0 #ffffff)
                        drop-shadow(0 2px 0 #ffffff)
                        drop-shadow(0 -2px 0 #ffffff)
                        drop-shadow(2px 2px 0 #ffffff)
                        drop-shadow(-2px -2px 0 #ffffff)
                        drop-shadow(2px -2px 0 #ffffff)
                        drop-shadow(-2px 2px 0 #ffffff)
                    `,
                    animation: "botFloat 2.2s ease-in-out infinite",
                    }}
                >
                    <Lottie
                    animationData={animaBot}
                    loop
                    autoplay
                    className="h-full w-full"
                    />
                </div>
                </div>

                {/* 로봇 바로 아래 멘트 */}
                <div className="mt-1 min-h-[32px] rounded-full bg-white/88 px-4 py-2 shadow-sm backdrop-blur-sm">
                <p
                    key={messageIndex}
                    className="text-center text-sm font-semibold text-[#4b5b79]"
                    style={{
                    animation: "loadingTextFade 1.7s ease-in-out",
                    whiteSpace: "nowrap",
                    }}
                >
                    {loadingMessages[messageIndex]}
                </p>
                </div>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}