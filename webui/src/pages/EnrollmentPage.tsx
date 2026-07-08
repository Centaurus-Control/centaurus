import { Copy, Ticket } from "lucide-react";
import { type ChangeEvent, type FormEvent, type ReactElement, useState } from "react";
import { type EnrollmentTokenResponse } from "../api/client";
import { type LanguageCode, type MessageKey } from "../i18n/messages";

export interface EnrollmentTokenFormState {
  suggestedName: string;
  expiresIn: string;
}

export interface EnrollmentBundleNotice {
  suggestedName: string | null;
  enrollmentBundle: string;
}

export interface EnrollmentPageProps {
  enrollmentBundleNotice: EnrollmentBundleNotice | null;
  enrollmentForm: EnrollmentTokenFormState;
  enrollmentTokens: EnrollmentTokenResponse[];
  isSubmitting: boolean;
  language: LanguageCode;
  t: (key: MessageKey) => string;
  onCreateEnrollmentToken: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onEnrollmentExpiresInChange: (event: ChangeEvent<HTMLSelectElement>) => void;
  onEnrollmentSuggestedNameChange: (event: ChangeEvent<HTMLInputElement>) => void;
}

export function EnrollmentPage({
  enrollmentBundleNotice,
  enrollmentForm,
  enrollmentTokens,
  isSubmitting,
  language,
  t,
  onCreateEnrollmentToken,
  onEnrollmentExpiresInChange,
  onEnrollmentSuggestedNameChange
}: EnrollmentPageProps): ReactElement {
  const [copyNotice, setCopyNotice] = useState<string | null>(null);

  async function handleCopyEnrollmentBundle(): Promise<void> {
    if (enrollmentBundleNotice === null) {
      return;
    }

    await navigator.clipboard.writeText(enrollmentBundleNotice.enrollmentBundle);
    setCopyNotice(t("common.copied"));
  }

  return (
    <>
      {enrollmentBundleNotice !== null ? (
        <section className="panel temporaryPasswordPanel">
          <div>
            <p className="eyebrow">{t("enrollment.bundle")}</p>
            <h2>{enrollmentBundleNotice.suggestedName ?? t("enrollment.tokens")}</h2>
          </div>
          <div className="copyBlock">
            <code>{enrollmentBundleNotice.enrollmentBundle}</code>
            <button className="secondaryButton iconTextButton" type="button" onClick={handleCopyEnrollmentBundle}>
              <Copy aria-hidden="true" size={18} />
              <span>{t("common.copy")}</span>
            </button>
          </div>
          {copyNotice !== null ? <p className="successText">{copyNotice}</p> : null}
          <p>{t("enrollment.bundleHint")}</p>
        </section>
      ) : null}

      <section className="panel adminPanel">
        <div className="panelTitle">
          <Ticket aria-hidden="true" size={22} />
          <span>{t("enrollment.tokens")}</span>
        </div>

        <form className="createUserForm" onSubmit={onCreateEnrollmentToken}>
          <label>
            <span>{t("enrollment.suggestedName")}</span>
            <input value={enrollmentForm.suggestedName} onChange={onEnrollmentSuggestedNameChange} />
          </label>
          <label>
            <span>{t("enrollment.expiresIn")}</span>
            <select value={enrollmentForm.expiresIn} onChange={onEnrollmentExpiresInChange}>
              <option value="PT1H">{t("enrollment.oneHour")}</option>
              <option value="P1D">{t("enrollment.oneDay")}</option>
              <option value="P7D">{t("enrollment.oneWeek")}</option>
            </select>
          </label>
          <button className="primaryButton iconTextButton" disabled={isSubmitting} type="submit">
            <Ticket aria-hidden="true" size={18} />
            <span>{t("enrollment.createToken")}</span>
          </button>
        </form>

        {enrollmentTokens.length === 0 ? (
          <p className="hintText">{t("enrollment.noTokens")}</p>
        ) : (
          <div className="usersTable">
            <div className="enrollmentTableHeader">
              <span>{t("enrollment.suggestedName")}</span>
              <span>{t("enrollment.expiresAt")}</span>
              <span>{t("enrollment.usedAt")}</span>
              <span>{t("enrollment.usedByAgent")}</span>
              <span>{t("enrollment.createdAt")}</span>
            </div>
            {enrollmentTokens.map((token: EnrollmentTokenResponse): ReactElement => (
              <div className="enrollmentTableRow" key={token.id}>
                <div className="userNameCell">
                  <strong>{token.suggestedName ?? "-"}</strong>
                  <span>{token.id}</span>
                </div>
                <span>{formatDateTime(token.expiresAt, language)}</span>
                <span>{token.usedAt === null ? t("enrollment.unused") : formatDateTime(token.usedAt, language)}</span>
                <span>{token.usedByAgentId ?? "-"}</span>
                <span>{formatDateTime(token.createdAt, language)}</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  );
}

function formatDateTime(value: string, language: LanguageCode): string {
  return new Intl.DateTimeFormat(language, {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
}
