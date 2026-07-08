import { type ChangeEvent, type FormEvent, type ReactElement } from "react";
import { type MessageKey } from "../i18n/messages";

export interface PasswordChangeFormState {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface PasswordChangePageProps {
  error: string | null;
  isChangingPassword: boolean;
  isRequired: boolean;
  t: (key: MessageKey) => string;
  value: PasswordChangeFormState;
  onCancel: () => void;
  onConfirmPasswordChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onCurrentPasswordChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onNewPasswordChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
}

export function PasswordChangePage({
  error,
  isChangingPassword,
  isRequired,
  t,
  value,
  onCancel,
  onConfirmPasswordChange,
  onCurrentPasswordChange,
  onNewPasswordChange,
  onSubmit
}: PasswordChangePageProps): ReactElement {
  return (
    <section className="authLayout inlineAuthLayout">
      <form className="panel loginPanel" onSubmit={onSubmit}>
        <div className="formIntro">
          <h2>{t("auth.changePassword")}</h2>
          <p>{isRequired ? t("auth.passwordChangeRequired") : t("auth.passwordPolicy")}</p>
        </div>
        <label>
          <span>{t("auth.currentPassword")}</span>
          <input
            autoComplete="current-password"
            type="password"
            value={value.currentPassword}
            onChange={onCurrentPasswordChange}
          />
        </label>
        <label>
          <span>{t("auth.newPassword")}</span>
          <input autoComplete="new-password" type="password" value={value.newPassword} onChange={onNewPasswordChange} />
        </label>
        <label>
          <span>{t("auth.confirmPassword")}</span>
          <input
            autoComplete="new-password"
            type="password"
            value={value.confirmPassword}
            onChange={onConfirmPasswordChange}
          />
        </label>
        <p className="hintText">{t("auth.passwordPolicy")}</p>
        {error !== null ? <p className="errorText">{error}</p> : null}
        <div className="formActions">
          {!isRequired ? (
            <button className="secondaryButton" disabled={isChangingPassword} type="button" onClick={onCancel}>
              {t("auth.cancelPasswordChange")}
            </button>
          ) : null}
          <button className="primaryButton" disabled={isChangingPassword} type="submit">
            {isChangingPassword ? t("common.loading") : t("auth.changePassword")}
          </button>
        </div>
      </form>
    </section>
  );
}
