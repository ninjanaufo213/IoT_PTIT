import { useState } from 'react'

// Select tự làm để có hiệu ứng xổ xuống (select gốc của trình duyệt không thể tạo hiệu ứng)
export default function Select({ value, options, onChange }) {
  const [open, setOpen] = useState(false)
  const current = options.find((o) => o.value === value)

  return (
    <div className="select" tabIndex={0} onBlur={() => setOpen(false)}>
      <div className="select-box" onClick={() => setOpen(!open)}>
        {current.label}
        <span className={open ? 'arrow up' : 'arrow'}>▾</span>
      </div>
      {open && (
        <ul>
          {options.map((o) => (
            <li key={o.value} className={o.value === value ? 'selected' : ''}
              onMouseDown={() => { onChange(o.value); setOpen(false) }}>
              {o.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
