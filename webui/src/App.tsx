import { type ReactElement } from "react";
import { useAppController } from "./app/useAppController";
import { AppHeader } from "./components/AppHeader";
import { DashboardPage } from "./pages/DashboardPage";
import { EnrollmentPage } from "./pages/EnrollmentPage";
import { LoginPage } from "./pages/LoginPage";
import { MachinesPage } from "./pages/MachinesPage";
import { PasswordChangePage } from "./pages/PasswordChangePage";
import { UsersPage } from "./pages/UsersPage";

export function App(): ReactElement {
  const controller = useAppController();

  return (
    <main className="appShell">
      <AppHeader
        activeView={controller.activeView}
        canUseNavigation={controller.canUseNavigation}
        isAccountMenuOpen={controller.isAccountMenuOpen}
        isAdmin={controller.isAdmin}
        isLoadingStatus={controller.isLoadingStatus}
        language={controller.language}
        t={controller.t}
        user={controller.session.user}
        onChangePassword={controller.handleOpenPasswordChange}
        onLanguageChange={controller.handleLanguageChange}
        onLogout={controller.handleLogout}
        onNavigateDashboard={controller.handleNavigateDashboard}
        onNavigateEnrollment={controller.handleNavigateEnrollment}
        onNavigateMachines={controller.handleNavigateMachines}
        onNavigateUsers={controller.handleNavigateUsers}
        onRefresh={controller.handleRefreshStatus}
        onToggleAccountMenu={controller.handleToggleAccountMenu}
      />

      {controller.isBootstrapping ? (
        <section className="panel compactPanel">{controller.t("common.loading")}</section>
      ) : controller.session.user === null ? (
        <LoginPage
          error={controller.error}
          health={controller.health}
          isSubmitting={controller.isSubmitting}
          notice={controller.notice}
          t={controller.t}
          value={controller.loginForm}
          onPasswordChange={controller.handleLoginPasswordChange}
          onSubmit={controller.handleLogin}
          onUsernameChange={controller.handleUsernameChange}
        />
      ) : (
        <section className="dashboard">
          {controller.shouldShowPasswordChangeForm ? (
            <PasswordChangePage
              error={controller.error}
              isChangingPassword={controller.isChangingPassword}
              isRequired={controller.mustChangePassword}
              t={controller.t}
              value={controller.passwordChangeForm}
              onCancel={controller.handleCancelPasswordChange}
              onConfirmPasswordChange={controller.handleConfirmPasswordChange}
              onCurrentPasswordChange={controller.handleCurrentPasswordChange}
              onNewPasswordChange={controller.handleNewPasswordChange}
              onSubmit={controller.handlePasswordChangeSubmit}
            />
          ) : null}

          {!controller.shouldShowPasswordChangeForm && controller.error !== null ? (
            <p className="errorText">{controller.error}</p>
          ) : null}
          {!controller.shouldShowPasswordChangeForm && controller.notice !== null ? (
            <p className="successText">{controller.notice}</p>
          ) : null}

          {!controller.mustChangePassword && controller.activeView === "dashboard" ? (
            <DashboardPage
              language={controller.language}
              machines={controller.machines}
              status={controller.status}
              t={controller.t}
              onSelectMachine={async (machineId: string): Promise<void> => {
                await controller.handleSelectMachine(machineId);
                controller.handleNavigateMachines();
              }}
            />
          ) : null}

          {!controller.mustChangePassword && controller.activeView === "machines" && controller.session.user !== null ? (
            <MachinesPage
              commands={controller.machineCommands}
              functions={controller.machineFunctions}
              language={controller.language}
              machines={controller.machines}
              machineRenameValue={controller.machineRenameValue}
              networkInterfaces={controller.machineNetworkInterfaces}
              role={controller.session.user.role}
              scriptButtonConfigurationForm={controller.scriptButtonConfigurationForm}
              scriptButtonConfigurations={controller.scriptButtonConfigurations}
              scripts={controller.machineScripts}
              selectedMachine={controller.selectedMachine}
              stats={controller.machineStats}
              statusChecks={controller.machineStatusChecks}
              statusCheckConfigurationForm={controller.statusCheckConfigurationForm}
              statusCheckConfigurations={controller.statusCheckConfigurations}
              t={controller.t}
              onAssignMachineFunction={controller.handleAssignMachineFunction}
              onCancelScriptButtonConfigurationEdit={controller.handleCancelScriptButtonConfigurationEdit}
              onCancelStatusCheckConfigurationEdit={controller.handleCancelStatusCheckConfigurationEdit}
              onDeleteScriptButtonConfiguration={controller.handleDeleteScriptButtonConfiguration}
              onDeleteStatusCheckConfiguration={controller.handleDeleteStatusCheckConfiguration}
              onDeleteMachine={controller.handleDeleteMachine}
              onEditScriptButtonConfiguration={controller.handleEditScriptButtonConfiguration}
              onEditStatusCheckConfiguration={controller.handleEditStatusCheckConfiguration}
              onExecuteMachineFunction={controller.handleExecuteMachineFunction}
              onExecuteScriptButtonConfiguration={controller.handleExecuteScriptButtonConfiguration}
              onMachineRenameValueChange={controller.handleMachineRenameValueChange}
              onRefreshScriptManifest={controller.handleRefreshScriptManifest}
              onRefreshStats={controller.handleRefreshMachineStats}
              onRenameMachine={controller.handleRenameMachine}
              onSaveScriptButtonConfiguration={controller.handleSaveScriptButtonConfiguration}
              onSaveStatusCheckConfiguration={controller.handleSaveStatusCheckConfiguration}
              onScriptButtonConfigurationFieldChange={controller.handleScriptButtonConfigurationFieldChange}
              onScriptButtonConfigurationParameterChange={controller.handleScriptButtonConfigurationParameterChange}
              onScriptButtonConfigurationScriptChange={controller.handleScriptButtonConfigurationScriptChange}
              onStatusCheckConfigurationFieldChange={controller.handleStatusCheckConfigurationFieldChange}
              onStatusCheckConfigurationParameterChange={controller.handleStatusCheckConfigurationParameterChange}
              onStatusCheckConfigurationScriptChange={controller.handleStatusCheckConfigurationScriptChange}
              onSelectMachine={controller.handleSelectMachine}
              onWakeOnLanInterfaceChange={controller.handleWakeOnLanInterfaceChange}
            />
          ) : null}

          {!controller.mustChangePassword && controller.isAdmin && controller.activeView === "users" ? (
            <UsersPage
              createUserForm={controller.createUserForm}
              isSubmitting={controller.isSubmitting}
              t={controller.t}
              temporaryPasswordNotice={controller.temporaryPasswordNotice}
              trustedCertificateForm={controller.trustedCertificateForm}
              trustedCertificates={controller.trustedCertificates}
              users={controller.users}
              onCancelTrustedCertificateEdit={controller.handleCancelTrustedCertificateEdit}
              onCreateUser={controller.handleCreateUser}
              onCreateUserRoleChange={controller.handleCreateUserRoleChange}
              onCreateUserUsernameChange={controller.handleCreateUserUsernameChange}
              onDeleteUser={controller.handleDeleteManagedUser}
              onDeleteTrustedCertificate={controller.handleDeleteTrustedCertificate}
              onEditTrustedCertificate={controller.handleEditTrustedCertificate}
              onResetPassword={controller.handleResetManagedUserPassword}
              onRoleChange={controller.handleManagedUserRoleChange}
              onTrustedCertificateFieldChange={controller.handleTrustedCertificateFieldChange}
              onTrustedCertificateSubmit={controller.handleTrustedCertificateSubmit}
            />
          ) : null}

          {!controller.mustChangePassword && controller.isAdmin && controller.activeView === "enrollment" ? (
            <EnrollmentPage
              enrollmentBundleNotice={controller.enrollmentBundleNotice}
              enrollmentForm={controller.enrollmentForm}
              enrollmentTokens={controller.enrollmentTokens}
              isSubmitting={controller.isSubmitting}
              language={controller.language}
              t={controller.t}
              onCreateEnrollmentToken={controller.handleCreateEnrollmentToken}
              onEnrollmentExpiresInChange={controller.handleEnrollmentExpiresInChange}
              onEnrollmentSuggestedNameChange={controller.handleEnrollmentSuggestedNameChange}
            />
          ) : null}
        </section>
      )}
    </main>
  );
}
