import { useParams } from 'react-router-dom'

export function KnowledgeBaseDetailPage() {
  const { id } = useParams()

  return (
    <section>
      <h1 className="text-2xl font-semibold">知识库详情</h1>
      <p className="mt-2 text-slate-600">当前知识库 ID：{id}</p>
    </section>
  )
}
