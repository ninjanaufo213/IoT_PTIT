import { useEffect, useState } from 'react'
import Select from './Select'

// Bảng dùng chung cho trang Data và History. Tìm kiếm, lọc, phân trang đều xử lý ở backend.
// load({ page, size, field, q, ...bộ lọc }) => { content, totalElements, totalPages }
// filters = [{ key, all: chữ hiển thị khi chưa chọn, options: [{ value, label }] }]
// searchFields = [{ value, label }]
const timeField = [{ value: 'time', label: 'Thời gian' }]
export default function DataTable({ title, load, columns, filters = [], searchFields = timeField, defaultSize }) {
  const [input, setInput] = useState('')
  const [field, setField] = useState(searchFields[0].value)
  const [applied, setApplied] = useState({ query: '', field }) // chỉ đổi khi bấm tìm kiếm
  const [chosen, setChosen] = useState({})
  const [size, setSize] = useState(defaultSize)
  const [page, setPage] = useState(1)
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 1 })
  const [error, setError] = useState('')

  // Tải lại khi đổi tham số, và tự làm mới mỗi 5 giây
  useEffect(() => {
    const fetchData = () =>
      load({ page, size, field: applied.field, q: applied.query, ...chosen })
        .then((d) => { setData(d); setError('') })
        .catch((e) => setError(e.message))
    fetchData()
    const timer = setInterval(fetchData, 5000)
    return () => clearInterval(timer)
  }, [page, size, applied, chosen])

  const pages = Math.max(1, data.totalPages)
  const search = () => { setApplied({ query: input, field }); setPage(1) }

  return (
    <>
      <div className="toolbar">
        <input className="search" placeholder={searchFields.length > 1 ? '🔍 Nhập từ khóa tìm kiếm...' : '🔍 Nhập ' + searchFields[0].label.toLowerCase() + ' để tìm kiếm'} value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && search()} />
        <button className="btn" onClick={search}>Tìm kiếm</button>
        {searchFields.length > 1 && (
          <div className="field">
            <span>Tìm kiếm theo :</span>
            <Select value={field} onChange={setField} options={searchFields} />
          </div>
        )}
        <span className="spacer" />
        {filters.map((f) => (
          <Select key={f.key} value={chosen[f.key] || ''}
            onChange={(v) => { setChosen({ ...chosen, [f.key]: v }); setPage(1) }}
            options={[{ value: '', label: f.all }, ...f.options]} />
        ))}
        <Select value={size} onChange={(v) => { setSize(v); setPage(1) }}
          options={[5, 10, 15, 20, 25, 30].map((n) => ({ value: n, label: n + ' Dòng' }))} />
      </div>

      <div className="card">
        {title && <h3>{title}</h3>}
        {error && <p className="red">⚠ {error}</p>}
        <table>
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key}>{c.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.content.map((r) => (
              <tr key={r.id}>
                {columns.map((c) => <td key={c.key}>{c.render ? c.render(r) : r[c.key]}</td>)}
              </tr>
            ))}
          </tbody>
        </table>

        <div className="table-foot">
          <small className="count">Hiển thị {data.content.length} / {data.totalElements} hàng</small>
          <div className="pagination">
            <button disabled={page === 1} onClick={() => setPage(page - 1)}>Previous</button>
            {pageList(page, pages).map((p, i) => p === '…'
              ? <span key={'gap' + i}>…</span>
              : <button key={p} className={page === p ? 'active' : ''} onClick={() => setPage(p)}>{p}</button>)}
            <button disabled={page >= pages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
          <span className="count" />
        </div>
      </div>
    </>
  )
}

// Dữ liệu tăng liên tục nên chỉ hiện trang đầu, cuối và vài trang quanh trang hiện tại
function pageList(current, total) {
  const set = new Set([1, total, current - 1, current, current + 1].filter((p) => p >= 1 && p <= total))
  const sorted = [...set].sort((a, b) => a - b)
  return sorted.flatMap((p, i) => (i && p - sorted[i - 1] > 1 ? ['…', p] : [p]))
}
