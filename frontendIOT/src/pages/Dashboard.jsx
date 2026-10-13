import { useEffect, useState } from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import Select from '../components/Select'
import { getChart, getLatest, getLedStatus, controlLed } from '../api'

const clamp = (x) => Math.min(1, Math.max(0, x))
const hsl = (h, s, a = 1) => `hsl(${h} ${s}% 50% / ${a})`

// Màu icon + số đổi theo giá trị (hàm hsl trả về [sắc độ, độ đậm]):
// nhiệt độ càng cao càng đỏ, ánh sáng càng mạnh càng vàng rực, độ ẩm càng cao càng xanh đậm
const Icon = ({ children }) => (
  <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    {children}
  </svg>
)

const stats = [
  {
    key: 'temperature', label: 'Temperature', unit: '°C',
    icon: <path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z" />,
    hsl: (v) => [v < 25 ? 210 : 50 - 50 * clamp((v - 25) / 15), 85], // dưới 25°C xanh, rồi vàng → cam → đỏ (40°C)
  },
  {
    key: 'light', label: 'Luminosity', unit: ' %',
    icon: <><circle cx="12" cy="12" r="5" /><path d="M12 1v2M12 21v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M1 12h2M21 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4" /></>,
    hsl: (v) => [45, 25 + 70 * clamp(v / 100)],
  },
  {
    key: 'humidity', label: 'Humidity', unit: '%',
    icon: <path d="M12 2.7l5.7 5.7a8 8 0 1 1-11.4 0z" />,
    hsl: (v) => [190 + 40 * clamp(v / 100), 85],
  },
]

const lines = [
  { value: 'temperature', label: 'Nhiệt độ', name: 'Temp', color: '#ef4444' },
  { value: 'light', label: 'Ánh sáng', name: 'Luminosity', color: '#f59e0b' },
  { value: 'humidity', label: 'Độ ẩm', name: 'Humidity', color: '#3b82f6' },
]

export default function Dashboard() {
  const [leds, setLeds] = useState([false, false, false])
  const [show, setShow] = useState('') // '' = hiện cả 3 đường
  const [latest, setLatest] = useState(null)
  const [chartData, setChartData] = useState([])
  const [error, setError] = useState('')
  const [pending, setPending] = useState({}) // { chỉ số LED: trạng thái đang chờ ESP32 xác nhận }

  // Cập nhật số liệu, biểu đồ và trạng thái LED mỗi 2 giây (bằng chu kỳ gửi của ESP32)
  const refresh = () =>
    Promise.all([getLatest(), getChart(20), getLedStatus()])
      .then(([l, c, st]) => {
        setLatest(l)
        setChartData(c.map((r) => ({ ...r, time: r.time.slice(-8) })))
        setLeds([st.led1, st.led2, st.led3])
        setError('')
      })
      .catch((e) => setError(e.message))

  useEffect(() => {
    refresh()
    const timer = setInterval(refresh, 2000)
    return () => clearInterval(timer)
  }, [])

  // Gửi lệnh rồi hỏi lại trạng thái liên tục (300ms) cho tới khi ESP32 xác nhận hoặc quá 6 giây
  const toggle = async (i) => {
    if (pending[i] !== undefined) return
    const target = !leds[i]
    setPending((p) => ({ ...p, [i]: target }))
    const done = () => setPending((p) => { const { [i]: _, ...rest } = p; return rest })
    try {
      await controlLed('led' + (i + 1), target ? 'on' : 'off')
      const deadline = Date.now() + 6000
      while (Date.now() < deadline) {
        await new Promise((r) => setTimeout(r, 300))
        const st = await getLedStatus()
        if (st['led' + (i + 1)] === target) {
          setLeds([st.led1, st.led2, st.led3])
          setError('')
          return done()
        }
      }
      setError(`Led ${i + 1} không phản hồi, thử lại sau`)
    } catch (e) {
      setError(e.message)
    }
    done()
  }

  return (
    <>
      {error && <p className="red">⚠ {error}</p>}
      <div className="stats">
        {stats.map((s) => {
          const value = latest ? latest[s.key] : null
          const [h, sat] = value == null ? [210, 10] : s.hsl(value)
          return (
            <div key={s.label} className="stat">
              <span className="stat-icon" style={{ background: hsl(h, sat, 0.18), color: hsl(h, sat) }}>
                <Icon>{s.icon}</Icon>
              </span>
              <div>
                <small>{s.label}</small>
                <strong style={{ color: hsl(h, sat) }}>{value == null ? '--' : value}{s.unit}</strong>
              </div>
            </div>
          )
        })}
      </div>

      <div className="grid">
        <div className="card">
          <div className="card-head">
            <h3>Home Environment Monitoring</h3>
            <Select value={show} onChange={setShow} options={[{ value: '', label: 'Tất cả' }, ...lines]} />
          </div>
          <ResponsiveContainer width="100%" height={600}>
            <LineChart data={chartData}>
              <CartesianGrid vertical={false} stroke="#eee" />
              <XAxis dataKey="time" tickLine={false} fontSize={11} />
              <YAxis tickLine={false} fontSize={11} />
              <Tooltip />
              <Legend iconType="square" iconSize={8} verticalAlign="top" align="right" />
              {lines.filter((l) => !show || l.value === show).map((l) => (
                <Line key={l.value} name={l.name} dataKey={l.value} stroke={l.color} isAnimationActive={false} dot={false} />
              ))}
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h3>Device Controls</h3>
          {leds.map((on, i) => {
            const loading = pending[i] !== undefined
            return (
              <label key={i} className={on ? 'device on' : 'device'}>
                <span className="bulb">💡</span>
                <span className="info">
                  <b>Led {i + 1}</b>
                  <small className={on ? 'state on' : 'state'}>
                    {loading ? (pending[i] ? 'Đang bật...' : 'Đang tắt...') : on ? 'ON' : 'OFF'}
                  </small>
                </span>
                {loading && <span className="spinner" />}
                <input type="checkbox" className="switch" checked={loading ? pending[i] : on} disabled={loading} onChange={() => toggle(i)} />
              </label>
            )
          })}
        </div>
      </div>
    </>
  )
}
