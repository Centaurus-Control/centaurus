import { FileKey2, Pencil, Plus, Trash2, X } from "lucide-react";
import { type ChangeEvent, type FormEvent, type ReactElement } from "react";
import { type TrustedCertificateResponse } from "../api/client";
import { type MessageKey } from "../i18n/messages";

export interface TrustedCertificateFormState {
  alias: string;
  certificateId: string | null;
  certificatePem: string;
  displayName: string;
  enabled: boolean;
}

export interface TrustedCertificatesPanelProps {
  certificates: TrustedCertificateResponse[];
  form: TrustedCertificateFormState;
  isSubmitting: boolean;
  t: (key: MessageKey) => string;
  onCancelEdit: () => void;
  onDeleteCertificate: (certificate: TrustedCertificateResponse) => Promise<void>;
  onEditCertificate: (certificate: TrustedCertificateResponse) => void;
  onFieldChange: (field: keyof TrustedCertificateFormState, value: string | boolean) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
}

export function TrustedCertificatesPanel({
  certificates,
  form,
  isSubmitting,
  t,
  onCancelEdit,
  onDeleteCertificate,
  onEditCertificate,
  onFieldChange,
  onSubmit
}: TrustedCertificatesPanelProps): ReactElement {
  const isEditing: boolean = form.certificateId !== null;

  function handleAliasChange(event: ChangeEvent<HTMLInputElement>): void {
    onFieldChange("alias", event.target.value);
  }

  function handleDisplayNameChange(event: ChangeEvent<HTMLInputElement>): void {
    onFieldChange("displayName", event.target.value);
  }

  function handlePemChange(event: ChangeEvent<HTMLTextAreaElement>): void {
    onFieldChange("certificatePem", event.target.value);
  }

  function handleEnabledChange(event: ChangeEvent<HTMLInputElement>): void {
    onFieldChange("enabled", event.target.checked);
  }

  return (
    <section className="panel adminPanel trustedCertificatesPanel">
      <div className="panelTitle">
        <FileKey2 aria-hidden="true" size={22} />
        <span>{t("certificates.title")}</span>
      </div>

      <form className="trustedCertificateForm" onSubmit={onSubmit}>
        <div className="certificateFormGrid">
          <label>
            <span>{t("certificates.alias")}</span>
            <input
              autoComplete="off"
              placeholder="company-root-ca"
              value={form.alias}
              onChange={handleAliasChange}
            />
          </label>
          <label>
            <span>{t("certificates.displayName")}</span>
            <input value={form.displayName} onChange={handleDisplayNameChange} />
          </label>
          <label className="toggleLabel">
            <input checked={form.enabled} type="checkbox" onChange={handleEnabledChange} />
            <span>{t("certificates.enabled")}</span>
          </label>
        </div>
        <label>
          <span>{t("certificates.pem")}</span>
          <textarea className="certificateTextarea" value={form.certificatePem} onChange={handlePemChange} />
        </label>
        <div className="certificateFormActions">
          <button className="primaryButton iconTextButton" disabled={isSubmitting} type="submit">
            <Plus aria-hidden="true" size={18} />
            <span>{isEditing ? t("certificates.save") : t("certificates.create")}</span>
          </button>
          {isEditing ? (
            <button className="secondaryButton iconTextButton" type="button" onClick={onCancelEdit}>
              <X aria-hidden="true" size={18} />
              <span>{t("certificates.cancelEdit")}</span>
            </button>
          ) : null}
        </div>
      </form>

      {certificates.length === 0 ? (
        <p className="hintText">{t("certificates.noCertificates")}</p>
      ) : (
        <div className="certificateList">
          {certificates.map((certificate: TrustedCertificateResponse): ReactElement => (
            <TrustedCertificateRow
              key={certificate.id}
              certificate={certificate}
              t={t}
              onDeleteCertificate={onDeleteCertificate}
              onEditCertificate={onEditCertificate}
            />
          ))}
        </div>
      )}
    </section>
  );
}

interface TrustedCertificateRowProps {
  certificate: TrustedCertificateResponse;
  t: (key: MessageKey) => string;
  onDeleteCertificate: (certificate: TrustedCertificateResponse) => Promise<void>;
  onEditCertificate: (certificate: TrustedCertificateResponse) => void;
}

function TrustedCertificateRow({
  certificate,
  t,
  onDeleteCertificate,
  onEditCertificate
}: TrustedCertificateRowProps): ReactElement {
  function handleEdit(): void {
    onEditCertificate(certificate);
  }

  function handleDelete(): void {
    void onDeleteCertificate(certificate);
  }

  return (
    <article className="certificateRow">
      <div className="certificateRowHeader">
        <div>
          <strong>{certificate.displayName}</strong>
          <span>{certificate.alias}</span>
        </div>
        <span className={certificate.enabled ? "statusPill enabledPill" : "statusPill disabledPill"}>
          {certificate.enabled ? t("certificates.enabled") : t("certificates.disabled")}
        </span>
      </div>
      <dl className="certificateMetadata">
        <div>
          <dt>{t("certificates.subject")}</dt>
          <dd>{certificate.subjectDn}</dd>
        </div>
        <div>
          <dt>{t("certificates.issuer")}</dt>
          <dd>{certificate.issuerDn}</dd>
        </div>
        <div>
          <dt>{t("certificates.validUntil")}</dt>
          <dd>{new Date(certificate.notAfter).toLocaleString(navigator.language)}</dd>
        </div>
        <div>
          <dt>{t("certificates.fingerprint")}</dt>
          <dd className="fingerprintText">{certificate.sha256Fingerprint}</dd>
        </div>
      </dl>
      <div className="rowActions">
        <button
          aria-label={t("certificates.edit")}
          className="iconButton"
          title={t("certificates.edit")}
          type="button"
          onClick={handleEdit}
        >
          <Pencil aria-hidden="true" size={18} />
        </button>
        <button
          aria-label={t("certificates.delete")}
          className="iconButton dangerButton"
          title={t("certificates.delete")}
          type="button"
          onClick={handleDelete}
        >
          <Trash2 aria-hidden="true" size={18} />
        </button>
      </div>
    </article>
  );
}
