import { createBrowserRouter, Navigate } from 'react-router-dom'

import { Layout } from '../components/Layout'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { AdminChatRecordsPage } from '../pages/admin/AdminChatRecordsPage'
import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage'
import { AdminFeedbackPage } from '../pages/admin/AdminFeedbackPage'
import { AdminLayout } from '../pages/admin/AdminLayout'
import { AdminUnmatchedQuestionsPage } from '../pages/admin/AdminUnmatchedQuestionsPage'
import { ChatPage } from '../pages/ChatPage'
import { KnowledgeBaseDetailPage } from '../pages/KnowledgeBaseDetailPage'
import { KnowledgeBaseListPage } from '../pages/KnowledgeBaseListPage'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/knowledge-bases" replace />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <Layout />,
        children: [
          {
            path: '/knowledge-bases',
            element: <KnowledgeBaseListPage />,
          },
          {
            path: '/knowledge-bases/:knowledgeBaseId/chat',
            element: <ChatPage />,
          },
          {
            path: '/knowledge-bases/:id',
            element: <KnowledgeBaseDetailPage />,
          },
          {
            path: '/chat',
            element: <ChatPage />,
          },
        ],
      },
    ],
  },
  {
    element: <ProtectedRoute requiredRole="ADMIN" />,
    children: [
      {
        path: '/admin',
        element: <AdminLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/admin/dashboard" replace />,
          },
          {
            path: 'dashboard',
            element: <AdminDashboardPage />,
          },
          {
            path: 'chat-records',
            element: <AdminChatRecordsPage />,
          },
          {
            path: 'feedback',
            element: <AdminFeedbackPage />,
          },
          {
            path: 'unmatched',
            element: <AdminUnmatchedQuestionsPage />,
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/knowledge-bases" replace />,
  },
])
