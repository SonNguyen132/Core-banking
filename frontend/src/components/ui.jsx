export function Alert({ kind = 'error', children }) {
  const styles = {
    error: 'bg-red-50 text-red-700 border-red-200',
    success: 'bg-green-50 text-green-700 border-green-200',
    info: 'bg-blue-50 text-blue-700 border-blue-200',
  }
  if (!children) return null
  return (
    <div className={`${styles[kind]} border rounded-lg px-4 py-3 text-sm mb-4`}>
      {children}
    </div>
  )
}

export function Spinner() {
  return (
    <div className="flex justify-center py-10">
      <div className="w-8 h-8 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )
}

export function EmptyState({ text = 'Chưa có dữ liệu' }) {
  return <p className="text-slate-400 text-sm py-8 text-center">{text}</p>
}

export const statusBadge = (status) => {
  const map = {
    PENDING: 'bg-amber-100 text-amber-700',
    ACTIVE: 'bg-green-100 text-green-700',
    COMPLETED: 'bg-green-100 text-green-700',
    FAILED: 'bg-red-100 text-red-700',
    APPROVED: 'bg-blue-100 text-blue-700',
    REPAYING: 'bg-indigo-100 text-indigo-700',
    DEFAULTED: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-slate-100 text-slate-600',
    DRAFT: 'bg-slate-100 text-slate-600',
  }
  return map[status] || 'bg-slate-100 text-slate-600'
}
