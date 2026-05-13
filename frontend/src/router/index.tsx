import { createBrowserRouter, Navigate } from 'react-router-dom'

import { Layout } from '../components/Layout'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { AdminDashboardPage } from '../pages/AdminDashboardPage'
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
          {
            path: '/admin',
            element: <AdminDashboardPage />,
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
