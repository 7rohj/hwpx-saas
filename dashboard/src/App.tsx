import { useEffect, useState } from "react"
import DashboardLoadingPage from "./pages/DashboardLoadingPage"
import Dashboard from "./pages/Dashboard"
import StudioPage from "./pages/StudioPage"
import ComposeStudioPage from "./pages/ComposeStudioPage"
import LandingPage from "./pages/LandingPage"
import ApiKeysPage from "./pages/ApiKeysPage"
import UsageLogsPage from "./pages/UsageLogsPage"
import PricingPage from "./pages/PricingPage"
import Login from "./Login"
import Signup from "./Signup"
import AdminLoginPage from "./pages/AdminLoginPage"
import AdminPage from "./pages/AdminPage"
import KoreanImageEditorPage from "./pages/KoreanImageEditorPage"
import OAuthSuccessPage from "./pages/OAuthSuccessPage"

type AuthView = "login" | "signup"

export type PageType =
  | "landing"
  | "dashboard-loading"
  | "dashboard"
  | "composeStudio"
  | "studio"
  | "minimal-compose"
  | "apikeys"
  | "usage"
  | "pricing"
  | "admin-login"
  | "admin"
  | "korean-image-editor"
  | "oauth-success"

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [authView, setAuthView] = useState<AuthView>("login")
  const [currentPage, setCurrentPage] = useState<PageType>("landing")

  useEffect(() => {
    const email = localStorage.getItem("userEmail")
    const apiKey = localStorage.getItem("apiKey")
    setIsLoggedIn(Boolean(email && apiKey))
  }, [])

  useEffect(() => {
    if (window.location.pathname === "/oauth-success") {
      setCurrentPage("oauth-success")
    }
  }, [])

  useEffect(() => {
    if (currentPage !== "dashboard-loading") return

    const timer = window.setTimeout(() => {
      setCurrentPage("studio")
    }, 3200)

    return () => window.clearTimeout(timer)
  }, [currentPage])

  function handleLoginSuccess() {
    setIsLoggedIn(true)
    setCurrentPage("landing")
  }

  function handleAdminLoginSuccess() {
    setCurrentPage("admin")
  }

  function handleSignupSuccess() {
    setIsLoggedIn(true)
    setCurrentPage("landing")
  }

  function handleLogout() {
    localStorage.removeItem("userEmail")
    localStorage.removeItem("apiKey")
    setIsLoggedIn(false)
    setAuthView("login")
    setCurrentPage("landing")
  }

  if (currentPage === "admin-login") {
    return (
      <AdminLoginPage
        onAdminLoginSuccess={handleAdminLoginSuccess}
        onBackToLogin={() => setAuthView("login")}
      />
    )
  }

  if (currentPage === "admin") {
    return <AdminPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "oauth-success") {
    return (
      <OAuthSuccessPage
        onNavigate={(page) => {
          setIsLoggedIn(Boolean(localStorage.getItem("userEmail") && localStorage.getItem("apiKey")))
          setCurrentPage(page)
        }}
      />
    )
  }

  if (!isLoggedIn) {
    if (authView === "signup") {
      return (
        <Signup
          onSignupSuccess={handleSignupSuccess}
          onGoLogin={() => setAuthView("login")}
        />
      )
    }

    return (
      <Login
        onLoginSuccess={handleLoginSuccess}
        onGoSignup={() => setAuthView("signup")}
        onGoAdminLogin={() => setCurrentPage("admin-login")}
      />
    )
  }

  if (currentPage === "dashboard-loading") {
    return (
      <DashboardLoadingPage
        onNavigate={setCurrentPage}
        onLogout={handleLogout}
      />
    )
  }

  if (currentPage === "landing") {
    return <LandingPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "dashboard") {
    return <Dashboard onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "studio") {
    return <StudioPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "composeStudio") {
    return (
      <ComposeStudioPage
        onNavigate={setCurrentPage}
        onLogout={handleLogout}
      />
    )
  }

  if (currentPage === "korean-image-editor") {
    return (
      <KoreanImageEditorPage
        onNavigate={setCurrentPage}
        onLogout={handleLogout}
      />
    )
  }

  if (currentPage === "apikeys") {
    return <ApiKeysPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "usage") {
    return <UsageLogsPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "pricing") {
    return <PricingPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  return <Dashboard onNavigate={setCurrentPage} onLogout={handleLogout} />
}