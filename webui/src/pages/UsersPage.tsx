import { type ChangeEvent, type FormEvent, type ReactElement } from "react";
import { type UserResponse, type UserRole } from "../api/client";
import { AdminUsersPanel, type CreateUserFormState } from "../components/AdminUsersPanel";
import { type MessageKey } from "../i18n/messages";

export interface TemporaryPasswordNotice {
  username: string;
  temporaryPassword: string;
}

export interface UsersPageProps {
  createUserForm: CreateUserFormState;
  isSubmitting: boolean;
  t: (key: MessageKey) => string;
  temporaryPasswordNotice: TemporaryPasswordNotice | null;
  users: UserResponse[];
  onCreateUser: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onCreateUserRoleChange: (event: ChangeEvent<HTMLSelectElement>) => void;
  onCreateUserUsernameChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onDeleteUser: (user: UserResponse) => Promise<void>;
  onResetPassword: (user: UserResponse) => Promise<void>;
  onRoleChange: (userId: string, role: UserRole) => Promise<void>;
}

export function UsersPage({
  createUserForm,
  isSubmitting,
  t,
  temporaryPasswordNotice,
  users,
  onCreateUser,
  onCreateUserRoleChange,
  onCreateUserUsernameChange,
  onDeleteUser,
  onResetPassword,
  onRoleChange
}: UsersPageProps): ReactElement {
  return (
    <>
      {temporaryPasswordNotice !== null ? (
        <section className="panel temporaryPasswordPanel">
          <div>
            <p className="eyebrow">{t("admin.temporaryPassword")}</p>
            <h2>{temporaryPasswordNotice.username}</h2>
          </div>
          <code>{temporaryPasswordNotice.temporaryPassword}</code>
          <p>{t("admin.temporaryPasswordHint")}</p>
        </section>
      ) : null}
      <AdminUsersPanel
        createUserForm={createUserForm}
        isSubmitting={isSubmitting}
        t={t}
        users={users}
        onCreateUser={onCreateUser}
        onCreateUserRoleChange={onCreateUserRoleChange}
        onCreateUserUsernameChange={onCreateUserUsernameChange}
        onDeleteUser={onDeleteUser}
        onResetPassword={onResetPassword}
        onRoleChange={onRoleChange}
      />
    </>
  );
}
