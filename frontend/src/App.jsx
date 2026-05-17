import { Routes, Route, Navigate } from 'react-router-dom'
import HomePage from './pages/HomePage.jsx'
import StashPage from './pages/StashPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/:code" element={<StashPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
