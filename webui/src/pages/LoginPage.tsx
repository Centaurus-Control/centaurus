import { type ChangeEvent, type FormEvent, type ReactElement } from "react";
import { type ActuatorHealthResponse } from "../api/client";
import { HealthPanel } from "../components/HealthPanel";
import { type MessageKey } from "../i18n/messages";

export interface LoginFormState {
  username: string;
  password: string;
}

export interface LoginPageProps {
  error: string | null;
  health: ActuatorHealthResponse | null;
  isSubmitting: boolean;
  notice: string | null;
  t: (key: MessageKey) => string;
  value: LoginFormState;
  onPasswordChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onUsernameChange: (event: ChangeEvent<HTMLInputElement>) => void;
}

export function LoginPage({
  error,
  health,
  isSubmitting,
  notice,
  t,
  value,
  onPasswordChange,
  onSubmit,
  onUsernameChange
}: LoginPageProps): ReactElement {
  return (
    <section className="authLayout">
      <form className="panel loginPanel" onSubmit={onSubmit}>
        <label>
          <span>{t("auth.username")}</span>
          <input autoComplete="username" value={value.username} onChange={onUsernameChange} />
        </label>
        <label>
          <span>{t("auth.password")}</span>
          <input
            autoComplete="current-password"
            type="password"
            value={value.password}
            onChange={onPasswordChange}
          />
        </label>
        {notice !== null ? <p className="successText">{notice}</p> : null}
        {error !== null ? <p className="errorText">{error}</p> : null}
        <button className="primaryButton" disabled={isSubmitting} type="submit">
          {isSubmitting ? t("common.loading") : t("auth.signIn")}
        </button>
      </form>
      <HealthPanel health={health} t={t} />
    </section>
  );
}
