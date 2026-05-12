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
import FeedbackEventPage from "./pages/FeedbackEventPage"

type AuthView = "login" | "signup"

export type PageType =
  | "landing"
  | "login"
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
  | "feedback"

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [authView, setAuthView] = useState<AuthView>("login")
  const [currentPage, setCurrentPage] = useState<PageType>("landing")

  useEffect(() => {
    const email = localStorage.getItem("userEmail")
    const apiKey = localStorage.getItem("apiKey")
    const loginType = localStorage.getItem("loginType")

    setIsLoggedIn(Boolean(email && (apiKey || loginType === "oauth")))
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

  async function handleLogout() {
    try {
      await fetch(`${import.meta.env.VITE_API_BASE_URL ?? "https://api.autohwpx.com"}/api/v1/auth/logout`, {
        method: "POST",
        credentials: "include",
      })
    } catch (err) {
      console.error(err)
    }

    localStorage.removeItem("userEmail")
    localStorage.removeItem("apiKey")
    localStorage.removeItem("loginType")
    localStorage.removeItem("lastLoginAt")

    setIsLoggedIn(false)
    setAuthView("login")
    setCurrentPage("landing")
  }

  if (currentPage === "admin-login") {
    return (
      <AdminLoginPage
        onAdminLoginSuccess={handleAdminLoginSuccess}
        onBackToLogin={() => {
          setAuthView("login")
          setCurrentPage("login")
        }}
      />
    )
  }

  if (currentPage === "admin") {
    return <AdminPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "oauth-success") {
    return (
      <OAuthSuccessPage
        onNavigate={setCurrentPage}
        onLoginSuccess={handleLoginSuccess}
      />
    )
  }

  if (!isLoggedIn) {
    if (authView === "signup") {
      return (
        <Signup
          onSignupSuccess={handleSignupSuccess}
          onGoLogin={() => {
            setAuthView("login")
            setCurrentPage("login")
          }}
        />
      )
    }

    if (currentPage === "login") {
      return (
        <Login
          onLoginSuccess={handleLoginSuccess}
          onGoSignup={() => setAuthView("signup")}
        />
      )
    }

    return <LandingPage
    onNavigate={setCurrentPage}
    onLogout={handleLogout}
    isLoggedIn={isLoggedIn}
  />
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
    return <LandingPage
    onNavigate={setCurrentPage}
    onLogout={handleLogout}
    isLoggedIn={isLoggedIn}
  />
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
  
  if (currentPage === "feedback") {
    return (
      <FeedbackEventPage
        onNavigate={setCurrentPage}
        onLogout={handleLogout}
        isLoggedIn={isLoggedIn}
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