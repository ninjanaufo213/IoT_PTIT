import DataTable from '../components/DataTable'
import { getSensorList } from '../api'

const color = { 'Nhiệt độ': 'red', 'Độ ẩm': 'blue', 'Ánh sáng': 'orange' }

const columns = [
  { key: 'id', label: 'ID', render: (r) => '#' + r.id },
  { key: 'sensor', label: 'Loại cảm biến' },
  { key: 'value', label: 'Giá trị', render: (r) => <b className={color[r.sensor]}>{r.value} {r.unit}</b> },
  { key: 'time', label: 'Thời gian' },
]

// Mục đầu là mặc định. Giá trị là tên cảm biến trong bảng sensors; "Tất cả" tìm trong mọi giá trị
const searchFields = [
  { value: 'all', label: 'Tất cả cảm biến' },
  ...['Nhiệt độ', 'Độ ẩm', 'Ánh sáng'].map((name) => ({ value: name, label: name })),
  { value: 'time', label: 'Thời gian' },
]

export default function Data() {
  return <DataTable load={getSensorList} columns={columns} defaultSize={15} searchFields={searchFields} />
}
