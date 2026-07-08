import {
  ChevronDown,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Monitor,
  RefreshCw,
  ShieldCheck,
  Ticket,
  Users
} from "lucide-react";
import { type ChangeEvent, type ReactElement } from "react";
import { type AuthenticatedUserResponse } from "../api/client";
import { type LanguageCode, type MessageKey } from "../i18n/messages";

export type ActiveView = "dashboard" | "machines" | "users" | "enrollment";

export interface AppHeaderProps {
  activeView: ActiveView;
  canUseNavigation: boolean;
  isAccountMenuOpen: boolean;
  isAdmin: boolean;
  isLoadingStatus: boolean;
  language: LanguageCode;
  t: (key: MessageKey) => string;
  user: AuthenticatedUserResponse | null;
  onChangePassword: () => void;
  onLanguageChange: (event: ChangeEvent<HTMLSelectElement>) => void;
  onLogout: () => Promise<void>;
  onNavigateDashboard: () => void;
  onNavigateEnrollment: () => void;
  onNavigateMachines: () => void;
  onNavigateUsers: () => void;
  onRefresh: () => Promise<void>;
  onToggleAccountMenu: () => void;
}

export function AppHeader({
  activeView,
  canUseNavigation,
  isAccountMenuOpen,
  isAdmin,
  isLoadingStatus,
  language,
  t,
  user,
  onChangePassword,
  onLanguageChange,
  onLogout,
  onNavigateDashboard,
  onNavigateEnrollment,
  onNavigateMachines,
  onNavigateUsers,
  onRefresh,
  onToggleAccountMenu
}: AppHeaderProps): ReactElement {
  function handleLogoutClick(): void {
    void onLogout();
  }

  function handleRefreshClick(): void {
    void onRefresh();
  }

  return (
    <header className="topBar">
      <div className="brand">
        <ShieldCheck aria-hidden="true" size={28} />
        <div>
          <h1>{t("app.title")}</h1>
          <p>{t("app.subtitle")}</p>
        </div>
      </div>

      {user !== null ? (
        <nav className="mainNav" aria-label="Main">
          <button
            className={activeView === "dashboard" ? "navButton activeNavButton" : "navButton"}
            disabled={!canUseNavigation}
            type="button"
            onClick={onNavigateDashboard}
          >
            <LayoutDashboard aria-hidden="true" size={18} />
            <span>{t("nav.dashboard")}</span>
          </button>
          <button
            className={activeView === "machines" ? "navButton activeNavButton" : "navButton"}
            disabled={!canUseNavigation}
            type="button"
            onClick={onNavigateMachines}
          >
            <Monitor aria-hidden="true" size={18} />
            <span>{t("nav.machines")}</span>
          </button>
          {isAdmin ? (
            <>
              <button
                className={activeView === "users" ? "navButton activeNavButton" : "navButton"}
                disabled={!canUseNavigation}
                type="button"
                onClick={onNavigateUsers}
              >
                <Users aria-hidden="true" size={18} />
                <span>{t("nav.users")}</span>
              </button>
              <button
                className={activeView === "enrollment" ? "navButton activeNavButton" : "navButton"}
                disabled={!canUseNavigation}
                type="button"
                onClick={onNavigateEnrollment}
              >
                <Ticket aria-hidden="true" size={18} />
                <span>{t("nav.enrollment")}</span>
              </button>
            </>
          ) : null}
        </nav>
      ) : null}

      <div className="topBarActions">
        <label className="languageSelect" aria-label={t("common.language")}>
          <select value={language} onChange={onLanguageChange}>
            <option value="en">EN</option>
            <option value="de">DE</option>
          </select>
        </label>

        {user !== null ? (
          <>
            <button
              aria-label={t("common.retry")}
              className="iconButton"
              disabled={isLoadingStatus || !canUseNavigation}
              title={t("common.retry")}
              type="button"
              onClick={handleRefreshClick}
            >
              <RefreshCw aria-hidden="true" size={20} />
            </button>
            <div className="accountMenu">
              <button
                aria-expanded={isAccountMenuOpen}
                aria-label={t("account.menu")}
                className="accountButton"
                type="button"
                onClick={onToggleAccountMenu}
              >
                <span>
                  <strong>{user.username}</strong>
                  <small>{t(`role.${user.role}` as MessageKey)}</small>
                </span>
                <ChevronDown aria-hidden="true" size={18} />
              </button>
              {isAccountMenuOpen ? (
                <div className="accountMenuPanel">
                  <button type="button" onClick={onChangePassword}>
                    <KeyRound aria-hidden="true" size={18} />
                    <span>{t("auth.changePassword")}</span>
                  </button>
                  <button type="button" onClick={handleLogoutClick}>
                    <LogOut aria-hidden="true" size={18} />
                    <span>{t("auth.signOut")}</span>
                  </button>
                </div>
              ) : null}
            </div>
          </>
        ) : null}
      </div>
    </header>
  );
}
