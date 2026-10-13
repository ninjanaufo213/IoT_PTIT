import DataTable from '../components/DataTable'
import { getHistory } from '../api'

const statusLabel = { SUCCESS: 'Thành công', PENDING: 'Đang xử lý', ERROR: 'Lỗi' }
const statusClass = { SUCCESS: 'ok', PENDING: 'pending', ERROR: 'error' }

const columns = [
  { key: 'id', label: 'ID', render: (r) => '#' + r.id },
  { key: 'led', label: 'Thiết bị', render: (r) => 'Led ' + r.led },
  { key: 'action', label: 'Hành động', render: (r) => <b className={r.action === 'ON' ? 'green' : 'red'}>{r.action === 'ON' ? 'Bật' : 'Tắt'}</b> },
  { key: 'status', label: 'Trạng thái', render: (r) => <span className={'badge ' + statusClass[r.status]}>{statusLabel[r.status]}</span> },
  { key: 'time', label: 'Thời gian' },
]

const filters = [
  { key: 'led', all: 'Tất cả thiết bị', options: [1, 2, 3].map((n) => ({ value: String(n), label: 'Led ' + n })) },
  { key: 'action', all: 'Tất cả hành động', options: [{ value: 'ON', label: 'Bật' }, { value: 'OFF', label: 'Tắt' }] },
  { key: 'status', all: 'Tất cả trạng thái', options: Object.entries(statusLabel).map(([value, label]) => ({ value, label })) },
]

export default function History() {
  return <DataTable title="Lịch sử bật/tắt thiết bị" load={getHistory} columns={columns} defaultSize={10} filters={filters} />
}
