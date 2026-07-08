import { Plus, RotateCcw, Trash2, Users } from "lucide-react";
import { type ChangeEvent, type FormEvent, type ReactElement } from "react";
import { type UserResponse, type UserRole } from "../api/client";
import { type MessageKey } from "../i18n/messages";

export interface CreateUserFormState {
  username: string;
  role: UserRole;
}

const userRoles: UserRole[] = ["ADMIN", "OPERATOR", "VIEWER"];

export interface AdminUsersPanelProps {
  createUserForm: CreateUserFormState;
  isSubmitting: boolean;
  t: (key: MessageKey) => string;
  users: UserResponse[];
  onCreateUser: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onCreateUserRoleChange: (event: ChangeEvent<HTMLSelectElement>) => void;
  onCreateUserUsernameChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onDeleteUser: (user: UserResponse) => Promise<void>;
  onResetPassword: (user: UserResponse) => Promise<void>;
  onRoleChange: (userId: string, role: UserRole) => Promise<void>;
}

export function AdminUsersPanel({
  createUserForm,
  isSubmitting,
  t,
  users,
  onCreateUser,
  onCreateUserRoleChange,
  onCreateUserUsernameChange,
  onDeleteUser,
  onResetPassword,
  onRoleChange
}: AdminUsersPanelProps): ReactElement {
  return (
    <section className="panel adminPanel">
      <div className="panelTitle">
        <Users aria-hidden="true" size={22} />
        <span>{t("admin.users")}</span>
      </div>

      <form className="createUserForm" onSubmit={onCreateUser}>
        <label>
          <span>{t("admin.username")}</span>
          <input value={createUserForm.username} onChange={onCreateUserUsernameChange} />
        </label>
        <label>
          <span>{t("admin.role")}</span>
          <select value={createUserForm.role} onChange={onCreateUserRoleChange}>
            {userRoles.map((role: UserRole): ReactElement => (
              <option key={role} value={role}>
                {t(`role.${role}` as MessageKey)}
              </option>
            ))}
          </select>
        </label>
        <button className="primaryButton iconTextButton" disabled={isSubmitting} type="submit">
          <Plus aria-hidden="true" size={18} />
          <span>{t("admin.createUser")}</span>
        </button>
      </form>

      {users.length === 0 ? (
        <p className="hintText">{t("admin.noUsers")}</p>
      ) : (
        <div className="usersTable">
          <div className="usersTableHeader">
            <span>{t("admin.username")}</span>
            <span>{t("admin.role")}</span>
            <span>{t("admin.passwordRequired")}</span>
            <span>{t("admin.updated")}</span>
            <span />
          </div>
          {users.map((user: UserResponse): ReactElement => (
            <ManagedUserRow
              key={user.id}
              t={t}
              user={user}
              onDeleteUser={onDeleteUser}
              onResetPassword={onResetPassword}
              onRoleChange={onRoleChange}
            />
          ))}
        </div>
      )}
    </section>
  );
}

interface ManagedUserRowProps {
  t: (key: MessageKey) => string;
  user: UserResponse;
  onDeleteUser: (user: UserResponse) => Promise<void>;
  onResetPassword: (user: UserResponse) => Promise<void>;
  onRoleChange: (userId: string, role: UserRole) => Promise<void>;
}

function ManagedUserRow({
  t,
  user,
  onDeleteUser,
  onResetPassword,
  onRoleChange
}: ManagedUserRowProps): ReactElement {
  function handleRoleChange(event: ChangeEvent<HTMLSelectElement>): void {
    void onRoleChange(user.id, event.target.value as UserRole);
  }

  function handleResetPassword(): void {
    void onResetPassword(user);
  }

  function handleDeleteUser(): void {
    void onDeleteUser(user);
  }

  return (
    <div className="usersTableRow">
      <div className="userNameCell">
        <strong>{user.username}</strong>
        <span>{user.id}</span>
      </div>
      <select value={user.role} onChange={handleRoleChange}>
        {userRoles.map((role: UserRole): ReactElement => (
          <option key={role} value={role}>
            {t(`role.${role}` as MessageKey)}
          </option>
        ))}
      </select>
      <span>{user.passwordChangeRequired ? t("common.yes") : t("common.no")}</span>
      <span>{new Date(user.updatedAt).toLocaleString(navigator.language)}</span>
      <div className="rowActions">
        <button
          aria-label={t("admin.resetPassword")}
          className="iconButton"
          title={t("admin.resetPassword")}
          type="button"
          onClick={handleResetPassword}
        >
          <RotateCcw aria-hidden="true" size={18} />
        </button>
        <button
          aria-label={t("admin.deleteUser")}
          className="iconButton dangerButton"
          title={t("admin.deleteUser")}
          type="button"
          onClick={handleDeleteUser}
        >
          <Trash2 aria-hidden="true" size={18} />
        </button>
      </div>
    </div>
  );
}
