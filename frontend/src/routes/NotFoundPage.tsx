import { Link } from 'react-router'
import { EmptyState } from '@/components/states'

export function NotFoundPage() {
  return (
    <EmptyState title="Không tìm thấy trang">
      Nội dung không tồn tại hoặc bạn không có quyền xem.{' '}
      <Link to="/" className="text-primary hover:underline">
        Về bảng tin
      </Link>
    </EmptyState>
  )
}
