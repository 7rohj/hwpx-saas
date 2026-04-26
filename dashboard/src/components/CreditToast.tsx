export default function CreditToast({
  show,
  message,
  subMessage,
}: {
  show: boolean
  message: string
  subMessage?: string
}) {
  if (!show) return null

  return (
    <div className="fixed bottom-6 right-6 z-[9999] w-[360px] rounded-2xl border border-slate-700 bg-[#111827] px-5 py-4 text-white shadow-2xl">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-yellow-400 text-lg font-black text-slate-900">
          C
        </div>

        <div className="min-w-0">
          <div className="text-sm font-semibold text-yellow-300">
            Credit Used
          </div>
          <div className="mt-1 text-base font-bold">
            {message}
          </div>
          {subMessage ? (
            <div className="mt-1 text-xs text-slate-300">
              {subMessage}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}