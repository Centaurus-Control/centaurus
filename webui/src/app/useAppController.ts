import { type ChangeEvent, type FormEvent, useCallback, useEffect, useState } from "react";
import {
  changePassword,
  assignMachineFunction,
  createEnrollmentToken,
  createScriptButtonConfiguration,
  createStatusCheckConfiguration,
  createTrustedCertificate,
  createUser,
  deleteAgent,
  deleteTrustedCertificate,
  deleteUser,
  deleteScriptButtonConfiguration,
  deleteStatusCheckConfiguration,
  executeMachineFunction,
  executeScriptButtonConfiguration,
  getActuatorHealth,
  getCurrentUser,
  listLatestMachineStatusChecks,
  getLatestMachineStats,
  getMachine,
  getServerStatus,
  listEnrollmentTokens,
  listMachineCommands,
  listMachineFunctions,
  listMachineNetworkInterfaces,
  listMachines,
  listMachineScripts,
  listScriptButtonConfigurations,
  listStatusCheckConfigurations,
  listTrustedCertificates,
  listUsers,
  login,
  logout,
  renameMachine,
  refreshAccessToken,
  refreshScriptManifestCommand,
  refreshStatsCommand,
  resetUserPassword,
  streamMachineEvents,
  updateScriptButtonConfiguration,
  updateStatusCheckConfiguration,
  updateTrustedCertificate,
  updateWakeOnLanConfiguration,
  updateUserRole,
  type ActuatorHealthResponse,
  type ApiError,
  type AuthenticatedUserResponse,
  type ChangePasswordRequest,
  type CommandResponse,
  type EnrollmentTokenResponse,
  type MachineFunctionResponse,
  type MachineFunctionType,
  type MachineNetworkInterfaceResponse,
  type MachineResponse,
  type MachineStatsLatestResponse,
  type MachineStatusCheckLatestResponse,
  type MachineStatusChangedEvent,
  type ServerStatusResponse,
  type ScriptButtonConfigurationRequest,
  type ScriptButtonConfigurationResponse,
  type ScriptDefinitionResponse,
  type StatusCheckConfigurationRequest,
  type StatusCheckConfigurationResponse,
  type TrustedCertificateRequest,
  type TrustedCertificateResponse,
  type UpdateWakeOnLanConfigurationRequest,
  type UserResponse,
  type UserRole
} from "../api/client";
import {
  clearStoredAccessToken,
  readStoredAccessToken,
  writeStoredAccessToken,
  type StoredAccessToken
} from "../auth/tokenStore";
import { type CreateUserFormState } from "../components/AdminUsersPanel";
import { type ActiveView } from "../components/AppHeader";
import { type TrustedCertificateFormState } from "../components/TrustedCertificatesPanel";
import { translate, type LanguageCode, type MessageKey } from "../i18n/messages";
import { type LoginFormState } from "../pages/LoginPage";
import { type ScriptButtonConfigurationFormState, type StatusCheckConfigurationFormState } from "../pages/MachinesPage";
import { type PasswordChangeFormState } from "../pages/PasswordChangePage";
import { type EnrollmentBundleNotice, type EnrollmentTokenFormState } from "../pages/EnrollmentPage";
import { type TemporaryPasswordNotice } from "../pages/UsersPage";

interface SessionState {
  accessToken: string | null;
  expiresAt: string | null;
  user: AuthenticatedUserResponse | null;
}

const initialLoginFormState: LoginFormState = {
  username: "admin",
  password: ""
};

const initialPasswordChangeFormState: PasswordChangeFormState = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: ""
};

const initialCreateUserFormState: CreateUserFormState = {
  username: "",
  role: "OPERATOR"
};

const initialTrustedCertificateFormState: TrustedCertificateFormState = {
  alias: "",
  certificateId: null,
  certificatePem: "",
  displayName: "",
  enabled: true
};

const initialEnrollmentTokenFormState: EnrollmentTokenFormState = {
  suggestedName: "",
  expiresIn: "PT1H"
};

const initialScriptButtonConfigurationFormState: ScriptButtonConfigurationFormState = {
  configurationId: null,
  enabled: true,
  label: "",
  parameterValues: {},
  scriptDefinitionId: "",
  sortOrder: "10"
};

const initialStatusCheckConfigurationFormState: StatusCheckConfigurationFormState = {
  configurationId: null,
  enabled: true,
  intervalSeconds: "30",
  label: "",
  parameterValues: {},
  scriptDefinitionId: "",
  sortOrder: "10"
};

const MACHINE_REFRESH_INTERVAL_MS = 5_000;

export function useAppController() {
  const [language, setLanguage] = useState<LanguageCode>("en");
  const [session, setSession] = useState<SessionState>({
    accessToken: null,
    expiresAt: null,
    user: null
  });
  const [loginForm, setLoginForm] = useState<LoginFormState>(initialLoginFormState);
  const [passwordChangeForm, setPasswordChangeForm] = useState<PasswordChangeFormState>(
    initialPasswordChangeFormState
  );
  const [createUserForm, setCreateUserForm] = useState<CreateUserFormState>(initialCreateUserFormState);
  const [enrollmentForm, setEnrollmentForm] = useState<EnrollmentTokenFormState>(initialEnrollmentTokenFormState);
  const [enrollmentTokens, setEnrollmentTokens] = useState<EnrollmentTokenResponse[]>([]);
  const [enrollmentBundleNotice, setEnrollmentBundleNotice] = useState<EnrollmentBundleNotice | null>(null);
  const [machines, setMachines] = useState<MachineResponse[]>([]);
  const [selectedMachine, setSelectedMachine] = useState<MachineResponse | null>(null);
  const [machineScripts, setMachineScripts] = useState<ScriptDefinitionResponse[]>([]);
  const [machineStats, setMachineStats] = useState<MachineStatsLatestResponse | null>(null);
  const [machineStatusChecks, setMachineStatusChecks] = useState<MachineStatusCheckLatestResponse[]>([]);
  const [machineNetworkInterfaces, setMachineNetworkInterfaces] = useState<MachineNetworkInterfaceResponse[]>([]);
  const [machineCommands, setMachineCommands] = useState<CommandResponse[]>([]);
  const [machineFunctions, setMachineFunctions] = useState<MachineFunctionResponse[]>([]);
  const [machineRenameValue, setMachineRenameValue] = useState<string>("");
  const [scriptButtonConfigurations, setScriptButtonConfigurations] = useState<ScriptButtonConfigurationResponse[]>([]);
  const [scriptButtonConfigurationForm, setScriptButtonConfigurationForm] =
    useState<ScriptButtonConfigurationFormState>(initialScriptButtonConfigurationFormState);
  const [statusCheckConfigurations, setStatusCheckConfigurations] = useState<StatusCheckConfigurationResponse[]>([]);
  const [statusCheckConfigurationForm, setStatusCheckConfigurationForm] =
    useState<StatusCheckConfigurationFormState>(initialStatusCheckConfigurationFormState);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [trustedCertificates, setTrustedCertificates] = useState<TrustedCertificateResponse[]>([]);
  const [trustedCertificateForm, setTrustedCertificateForm] = useState<TrustedCertificateFormState>(
    initialTrustedCertificateFormState
  );
  const [temporaryPasswordNotice, setTemporaryPasswordNotice] = useState<TemporaryPasswordNotice | null>(null);
  const [status, setStatus] = useState<ServerStatusResponse | null>(null);
  const [health, setHealth] = useState<ActuatorHealthResponse | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [isChangingPassword, setIsChangingPassword] = useState<boolean>(false);
  const [isLoadingStatus, setIsLoadingStatus] = useState<boolean>(false);
  const [isPasswordChangeOpen, setIsPasswordChangeOpen] = useState<boolean>(false);
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState<boolean>(false);
  const [activeView, setActiveView] = useState<ActiveView>("dashboard");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const t = useCallback(
    (key: MessageKey): string => {
      return translate(language, key);
    },
    [language]
  );
  const selectedMachineId: string | null = selectedMachine?.id ?? null;
  const passwordChangeRequired: boolean = session.user?.passwordChangeRequired ?? false;

  const loadApplicationState = useCallback(async (token: StoredAccessToken): Promise<void> => {
    const [userResponse, healthResponse]: [AuthenticatedUserResponse, ActuatorHealthResponse] = await Promise.all([
      getCurrentUser(token.accessToken),
      getActuatorHealth()
    ]);

    setSession({
      accessToken: token.accessToken,
      expiresAt: token.expiresAt,
      user: userResponse
    });
    setHealth(healthResponse);

    if (userResponse.passwordChangeRequired) {
      setStatus(null);
      return;
    }

    const [statusResponse, machineResponses]: [ServerStatusResponse, MachineResponse[]] = await Promise.all([
      getServerStatus(token.accessToken),
      listMachines(token.accessToken)
    ]);
    setStatus(statusResponse);
    setMachines(machineResponses);

    if (userResponse.role === "ADMIN") {
      const [userResponses, tokenResponses, certificateResponses]: [
        UserResponse[],
        EnrollmentTokenResponse[],
        TrustedCertificateResponse[]
      ] = await Promise.all([
        listUsers(token.accessToken),
        listEnrollmentTokens(token.accessToken),
        listTrustedCertificates(token.accessToken)
      ]);
      setUsers(userResponses);
      setEnrollmentTokens(tokenResponses);
      setTrustedCertificates(certificateResponses);
    } else {
      setUsers([]);
      setEnrollmentTokens([]);
      setTrustedCertificates([]);
    }
  }, []);

  const loadPublicHealth = useCallback(async (): Promise<void> => {
    const healthResponse: ActuatorHealthResponse = await getActuatorHealth();
    setHealth(healthResponse);
  }, []);

  useEffect(() => {
    let isActive: boolean = true;

    async function bootstrap(): Promise<void> {
      const storedToken: StoredAccessToken | null = readStoredAccessToken();

      try {
        if (storedToken !== null) {
          await loadApplicationState(storedToken);
          return;
        }

        const refreshedToken: StoredAccessToken = await refreshAccessToken();
        writeStoredAccessToken(refreshedToken);
        await loadApplicationState(refreshedToken);
      } catch {
        clearStoredAccessToken();
        if (isActive) {
          setSession({
            accessToken: null,
            expiresAt: null,
            user: null
          });
          await loadPublicHealth().catch((): void => {
            setHealth(null);
          });
        }
      } finally {
        if (isActive) {
          setIsBootstrapping(false);
        }
      }
    }

    void bootstrap();

    return (): void => {
      isActive = false;
    };
  }, [loadApplicationState, loadPublicHealth]);

  async function handleLogin(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    setNotice(null);

    try {
      const token: StoredAccessToken = await login(loginForm);
      writeStoredAccessToken(token);
      await loadApplicationState(token);
      setPasswordChangeForm(initialPasswordChangeFormState);
      setIsPasswordChangeOpen(false);
    } catch (loginError: unknown) {
      clearStoredAccessToken();
      setError(getErrorMessage(loginError));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleRefreshStatus(): Promise<void> {
    if (session.accessToken === null) {
      return;
    }

    setIsLoadingStatus(true);
    setError(null);

    try {
      const statusResponse: ServerStatusResponse = await getServerStatus(session.accessToken);
      const healthResponse: ActuatorHealthResponse = await getActuatorHealth();
      const machineResponses: MachineResponse[] = await listMachines(session.accessToken);
      setStatus(statusResponse);
      setHealth(healthResponse);
      setMachines(machineResponses);
      if (selectedMachine !== null) {
        await loadMachineDetails(selectedMachine.id, session.accessToken);
      }
      if (session.user?.role === "ADMIN") {
        const [userResponses, tokenResponses, certificateResponses]: [
          UserResponse[],
          EnrollmentTokenResponse[],
          TrustedCertificateResponse[]
        ] = await Promise.all([
          listUsers(session.accessToken),
          listEnrollmentTokens(session.accessToken),
          listTrustedCertificates(session.accessToken)
        ]);
        setUsers(userResponses);
        setEnrollmentTokens(tokenResponses);
        setTrustedCertificates(certificateResponses);
      }
    } catch (statusError: unknown) {
      setError(getErrorMessage(statusError));
    } finally {
      setIsLoadingStatus(false);
    }
  }

  async function handleLogout(): Promise<void> {
    setError(null);
    setNotice(null);

    try {
      await logout(session.accessToken);
    } finally {
      clearStoredAccessToken();
      resetAuthenticatedState();
      await loadPublicHealth().catch((): void => {
        setHealth(null);
      });
    }
  }

  async function handlePasswordChangeSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();

    if (session.accessToken === null || session.user === null) {
      return;
    }

    const validationError: string | null = validatePasswordChangeForm(passwordChangeForm, session.user.username, t);
    if (validationError !== null) {
      setError(validationError);
      return;
    }

    setIsChangingPassword(true);
    setError(null);
    setNotice(null);

    try {
      const request: ChangePasswordRequest = {
        currentPassword: passwordChangeForm.currentPassword,
        newPassword: passwordChangeForm.newPassword
      };
      await changePassword(request, session.accessToken);
      clearStoredAccessToken();
      resetAuthenticatedState();
      setLoginForm((currentForm: LoginFormState): LoginFormState => ({
        ...currentForm,
        password: ""
      }));
      setNotice(t("auth.passwordChanged"));
      await loadPublicHealth().catch((): void => {
        setHealth(null);
      });
    } catch (passwordChangeError: unknown) {
      setError(getErrorMessage(passwordChangeError));
    } finally {
      setIsChangingPassword(false);
    }
  }

  function resetAuthenticatedState(): void {
    setSession({
      accessToken: null,
      expiresAt: null,
      user: null
    });
    setStatus(null);
    setPasswordChangeForm(initialPasswordChangeFormState);
    setCreateUserForm(initialCreateUserFormState);
    setEnrollmentForm(initialEnrollmentTokenFormState);
    setEnrollmentBundleNotice(null);
    setEnrollmentTokens([]);
    setMachines([]);
    setSelectedMachine(null);
    setMachineScripts([]);
    setMachineStats(null);
    setMachineStatusChecks([]);
    setMachineNetworkInterfaces([]);
    setMachineCommands([]);
    setMachineFunctions([]);
    setMachineRenameValue("");
    setScriptButtonConfigurations([]);
    setScriptButtonConfigurationForm(initialScriptButtonConfigurationFormState);
    setStatusCheckConfigurations([]);
    setStatusCheckConfigurationForm(initialStatusCheckConfigurationFormState);
    setTemporaryPasswordNotice(null);
    setUsers([]);
    setTrustedCertificates([]);
    setTrustedCertificateForm(initialTrustedCertificateFormState);
    setIsPasswordChangeOpen(false);
    setIsAccountMenuOpen(false);
    setActiveView("dashboard");
  }

  function handleLanguageChange(event: ChangeEvent<HTMLSelectElement>): void {
    setLanguage(event.target.value as LanguageCode);
  }

  function handleUsernameChange(event: ChangeEvent<HTMLInputElement>): void {
    setLoginForm((currentForm: LoginFormState): LoginFormState => ({
      ...currentForm,
      username: event.target.value
    }));
  }

  function handleLoginPasswordChange(event: ChangeEvent<HTMLInputElement>): void {
    setLoginForm((currentForm: LoginFormState): LoginFormState => ({
      ...currentForm,
      password: event.target.value
    }));
  }

  function handleCurrentPasswordChange(event: ChangeEvent<HTMLInputElement>): void {
    setPasswordChangeForm((currentForm: PasswordChangeFormState): PasswordChangeFormState => ({
      ...currentForm,
      currentPassword: event.target.value
    }));
  }

  function handleNewPasswordChange(event: ChangeEvent<HTMLInputElement>): void {
    setPasswordChangeForm((currentForm: PasswordChangeFormState): PasswordChangeFormState => ({
      ...currentForm,
      newPassword: event.target.value
    }));
  }

  function handleConfirmPasswordChange(event: ChangeEvent<HTMLInputElement>): void {
    setPasswordChangeForm((currentForm: PasswordChangeFormState): PasswordChangeFormState => ({
      ...currentForm,
      confirmPassword: event.target.value
    }));
  }

  function handleCreateUserUsernameChange(event: ChangeEvent<HTMLInputElement>): void {
    setCreateUserForm((currentForm: CreateUserFormState): CreateUserFormState => ({
      ...currentForm,
      username: event.target.value
    }));
  }

  function handleCreateUserRoleChange(event: ChangeEvent<HTMLSelectElement>): void {
    setCreateUserForm((currentForm: CreateUserFormState): CreateUserFormState => ({
      ...currentForm,
      role: event.target.value as UserRole
    }));
  }

  function handleEnrollmentSuggestedNameChange(event: ChangeEvent<HTMLInputElement>): void {
    setEnrollmentForm((currentForm: EnrollmentTokenFormState): EnrollmentTokenFormState => ({
      ...currentForm,
      suggestedName: event.target.value
    }));
  }

  function handleEnrollmentExpiresInChange(event: ChangeEvent<HTMLSelectElement>): void {
    setEnrollmentForm((currentForm: EnrollmentTokenFormState): EnrollmentTokenFormState => ({
      ...currentForm,
      expiresIn: event.target.value
    }));
  }

  function handleTrustedCertificateFieldChange(
    field: keyof TrustedCertificateFormState,
    value: string | boolean
  ): void {
    setTrustedCertificateForm((currentForm: TrustedCertificateFormState): TrustedCertificateFormState => ({
      ...currentForm,
      [field]: value
    }));
  }

  function handleOpenPasswordChange(): void {
    setPasswordChangeForm(initialPasswordChangeFormState);
    setError(null);
    setNotice(null);
    setIsAccountMenuOpen(false);
    setActiveView("dashboard");
    setIsPasswordChangeOpen(true);
  }

  function handleCancelPasswordChange(): void {
    setPasswordChangeForm(initialPasswordChangeFormState);
    setError(null);
    setIsPasswordChangeOpen(false);
  }

  function handleNavigateDashboard(): void {
    setError(null);
    setNotice(null);
    setIsAccountMenuOpen(false);
    setActiveView("dashboard");
  }

  function handleNavigateUsers(): void {
    setError(null);
    setNotice(null);
    setIsAccountMenuOpen(false);
    setIsPasswordChangeOpen(false);
    setActiveView("users");
  }

  function handleNavigateEnrollment(): void {
    setError(null);
    setNotice(null);
    setIsAccountMenuOpen(false);
    setIsPasswordChangeOpen(false);
    setActiveView("enrollment");
  }

  function handleNavigateMachines(): void {
    setError(null);
    setNotice(null);
    setIsAccountMenuOpen(false);
    setIsPasswordChangeOpen(false);
    setActiveView("machines");
  }

  function handleToggleAccountMenu(): void {
    setIsAccountMenuOpen((currentValue: boolean): boolean => !currentValue);
  }

  async function handleCreateUser(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();

    if (session.accessToken === null) {
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setNotice(null);
    setTemporaryPasswordNotice(null);

    try {
      const createdUser: { user: UserResponse; temporaryPassword: string } = await createUser(
        {
          username: createUserForm.username,
          role: createUserForm.role
        },
        session.accessToken
      );
      setUsers((currentUsers: UserResponse[]): UserResponse[] => [...currentUsers, createdUser.user].sort(compareUsers));
      setCreateUserForm(initialCreateUserFormState);
      setTemporaryPasswordNotice({
        username: createdUser.user.username,
        temporaryPassword: createdUser.temporaryPassword
      });
      setActiveView("users");
      setNotice(t("admin.userCreated"));
    } catch (createUserError: unknown) {
      setError(getErrorMessage(createUserError));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleCreateEnrollmentToken(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();

    if (session.accessToken === null) {
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setNotice(null);
    setEnrollmentBundleNotice(null);

    try {
      const createdToken = await createEnrollmentToken(
        {
          suggestedName: enrollmentForm.suggestedName.trim() === "" ? null : enrollmentForm.suggestedName.trim(),
          expiresIn: enrollmentForm.expiresIn
        },
        session.accessToken
      );
      setEnrollmentTokens((currentTokens: EnrollmentTokenResponse[]): EnrollmentTokenResponse[] => [
        createdToken.token,
        ...currentTokens
      ]);
      setEnrollmentForm(initialEnrollmentTokenFormState);
      setEnrollmentBundleNotice({
        suggestedName: createdToken.token.suggestedName,
        enrollmentBundle: createdToken.enrollmentBundle
      });
      setActiveView("enrollment");
      setNotice(t("enrollment.tokenCreated"));
    } catch (createTokenError: unknown) {
      setError(getErrorMessage(createTokenError));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleTrustedCertificateSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();

    if (session.accessToken === null) {
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setNotice(null);

    try {
      const request: TrustedCertificateRequest = {
        alias: trustedCertificateForm.alias,
        certificatePem: trustedCertificateForm.certificatePem,
        displayName: trustedCertificateForm.displayName,
        enabled: trustedCertificateForm.enabled
      };
      const savedCertificate: TrustedCertificateResponse =
        trustedCertificateForm.certificateId === null
          ? await createTrustedCertificate(request, session.accessToken)
          : await updateTrustedCertificate(trustedCertificateForm.certificateId, request, session.accessToken);
      setTrustedCertificates((currentCertificates: TrustedCertificateResponse[]): TrustedCertificateResponse[] =>
        upsertTrustedCertificate(currentCertificates, savedCertificate).sort(compareTrustedCertificates)
      );
      setTrustedCertificateForm(initialTrustedCertificateFormState);
      setNotice(t("certificates.saved"));
    } catch (certificateError: unknown) {
      setError(getErrorMessage(certificateError));
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleEditTrustedCertificate(certificate: TrustedCertificateResponse): void {
    setTrustedCertificateForm({
      alias: certificate.alias,
      certificateId: certificate.id,
      certificatePem: certificate.certificatePem,
      displayName: certificate.displayName,
      enabled: certificate.enabled
    });
  }

  function handleCancelTrustedCertificateEdit(): void {
    setTrustedCertificateForm(initialTrustedCertificateFormState);
  }

  async function handleDeleteTrustedCertificate(certificate: TrustedCertificateResponse): Promise<void> {
    if (session.accessToken === null || !window.confirm(t("certificates.confirmDelete"))) {
      return;
    }

    setError(null);
    setNotice(null);

    try {
      await deleteTrustedCertificate(certificate.id, session.accessToken);
      setTrustedCertificates((currentCertificates: TrustedCertificateResponse[]): TrustedCertificateResponse[] =>
        currentCertificates.filter((currentCertificate: TrustedCertificateResponse): boolean => currentCertificate.id !== certificate.id)
      );
      if (trustedCertificateForm.certificateId === certificate.id) {
        setTrustedCertificateForm(initialTrustedCertificateFormState);
      }
      setNotice(t("certificates.deleted"));
    } catch (deleteError: unknown) {
      setError(getErrorMessage(deleteError));
    }
  }

  async function handleSelectMachine(machineId: string): Promise<void> {
    if (session.accessToken === null) {
      return;
    }

    setError(null);
    setNotice(null);
    await loadMachineDetails(machineId, session.accessToken);
  }

  const refreshMachines = useCallback(
    async (options: { includeSelectedMachine: boolean }): Promise<void> => {
      if (session.accessToken === null || session.user === null || passwordChangeRequired) {
        return;
      }

      const machineResponses: MachineResponse[] = await listMachines(session.accessToken);
      setMachines(machineResponses);

      if (!options.includeSelectedMachine || selectedMachineId === null) {
        return;
      }

      const selectedMachineSummary: MachineResponse | undefined = machineResponses.find(
        (machine: MachineResponse): boolean => machine.id === selectedMachineId
      );

      if (selectedMachineSummary === undefined) {
        setSelectedMachine(null);
        setMachineRenameValue("");
        setMachineScripts([]);
        setMachineStats(null);
        setMachineStatusChecks([]);
        setMachineNetworkInterfaces([]);
        setMachineCommands([]);
        setMachineFunctions([]);
        setScriptButtonConfigurations([]);
        setScriptButtonConfigurationForm(initialScriptButtonConfigurationFormState);
        setStatusCheckConfigurations([]);
        setStatusCheckConfigurationForm(initialStatusCheckConfigurationFormState);
        return;
      }

      const [machine, commands] = await Promise.all([
        getMachine(selectedMachineId, session.accessToken),
        listMachineCommands(selectedMachineId, session.accessToken)
      ]);

      setSelectedMachine(machine);
      setMachines((currentMachines: MachineResponse[]): MachineResponse[] => upsertMachine(currentMachines, machine));
      setMachineCommands(commands);

      try {
        const stats: MachineStatsLatestResponse = await getLatestMachineStats(selectedMachineId, session.accessToken);
        setMachineStats(stats);
      } catch {
        setMachineStats(null);
      }
      try {
        const statusChecks: MachineStatusCheckLatestResponse[] = await listLatestMachineStatusChecks(
          selectedMachineId,
          session.accessToken
        );
        setMachineStatusChecks(statusChecks);
      } catch {
        setMachineStatusChecks([]);
      }
    },
    [passwordChangeRequired, selectedMachineId, session.accessToken, session.user]
  );

  const refreshMachineFromStatusEvent = useCallback(
    async (event: MachineStatusChangedEvent): Promise<void> => {
      if (session.accessToken === null || session.user === null || passwordChangeRequired) {
        return;
      }

      const machine: MachineResponse = await getMachine(event.machineId, session.accessToken);
      setMachines((currentMachines: MachineResponse[]): MachineResponse[] => upsertMachine(currentMachines, machine));

      if (selectedMachineId !== event.machineId) {
        return;
      }

      setSelectedMachine(machine);
      const commands: CommandResponse[] = await listMachineCommands(event.machineId, session.accessToken);
      setMachineCommands(commands);

      try {
        const stats: MachineStatsLatestResponse = await getLatestMachineStats(event.machineId, session.accessToken);
        setMachineStats(stats);
      } catch {
        setMachineStats(null);
      }
      try {
        const statusChecks: MachineStatusCheckLatestResponse[] = await listLatestMachineStatusChecks(event.machineId, session.accessToken);
        setMachineStatusChecks(statusChecks);
      } catch {
        setMachineStatusChecks([]);
      }
    },
    [passwordChangeRequired, selectedMachineId, session.accessToken, session.user]
  );

  useEffect(() => {
    if (session.accessToken === null || session.user === null || passwordChangeRequired) {
      return;
    }

    const accessToken: string = session.accessToken;
    const abortController: AbortController = new AbortController();
    let reconnectTimeoutId: number | null = null;

    function connect(): void {
      void (async (): Promise<void> => {
        try {
          await streamMachineEvents(
            accessToken,
            abortController.signal,
            (event: MachineStatusChangedEvent): void => {
              void refreshMachineFromStatusEvent(event).catch((): void => {
                // The polling fallback will reconcile missed event updates.
              });
            }
          );
        } catch (streamError: unknown) {
          if (abortController.signal.aborted || isAbortError(streamError)) {
            return;
          }
        }

        if (!abortController.signal.aborted) {
          reconnectTimeoutId = window.setTimeout(connect, MACHINE_REFRESH_INTERVAL_MS);
        }
      })();
    }

    connect();

    return (): void => {
      abortController.abort();
      if (reconnectTimeoutId !== null) {
        window.clearTimeout(reconnectTimeoutId);
      }
    };
  }, [passwordChangeRequired, refreshMachineFromStatusEvent, session.accessToken, session.user]);

  useEffect(() => {
    if (
      activeView !== "machines" ||
      session.accessToken === null ||
      session.user === null ||
      passwordChangeRequired
    ) {
      return;
    }

    let isActive: boolean = true;
    let isRefreshing: boolean = false;

    async function refreshIfIdle(): Promise<void> {
      if (isRefreshing || document.visibilityState === "hidden") {
        return;
      }

      isRefreshing = true;
      try {
        await refreshMachines({ includeSelectedMachine: true });
      } catch {
        // Background refresh failures should not replace visible action errors.
      } finally {
        isRefreshing = false;
      }
    }

    void refreshIfIdle();
    const intervalId: number = window.setInterval((): void => {
      if (isActive) {
        void refreshIfIdle();
      }
    }, MACHINE_REFRESH_INTERVAL_MS);

    return (): void => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, [activeView, passwordChangeRequired, refreshMachines, session.accessToken, session.user]);

  async function loadMachineDetails(machineId: string, accessToken: string): Promise<void> {
    const [machine, scripts, networkInterfaces, commands, functions, configurations, statusConfigurations, statusChecks] = await Promise.all([
      getMachine(machineId, accessToken),
      listMachineScripts(machineId, accessToken),
      listMachineNetworkInterfaces(machineId, accessToken),
      listMachineCommands(machineId, accessToken),
      listMachineFunctions(machineId, accessToken),
      listScriptButtonConfigurations(machineId, accessToken),
      listStatusCheckConfigurations(machineId, accessToken),
      listLatestMachineStatusChecks(machineId, accessToken).catch((): MachineStatusCheckLatestResponse[] => [])
    ]);

    setSelectedMachine(machine);
    setMachineRenameValue(machine.displayName);
    setMachines((currentMachines: MachineResponse[]): MachineResponse[] => upsertMachine(currentMachines, machine));
    setMachineScripts(scripts);
    setMachineNetworkInterfaces(networkInterfaces);
    setMachineCommands(commands);
    setMachineFunctions(functions);
    setMachineStatusChecks(statusChecks);
    setScriptButtonConfigurations(configurations.sort(compareScriptButtonConfigurations));
    setStatusCheckConfigurations(statusConfigurations.sort(compareStatusCheckConfigurations));
    setScriptButtonConfigurationForm((currentForm: ScriptButtonConfigurationFormState): ScriptButtonConfigurationFormState =>
      normalizeScriptButtonConfigurationForm(currentForm, scripts)
    );
    setStatusCheckConfigurationForm((currentForm: StatusCheckConfigurationFormState): StatusCheckConfigurationFormState =>
      normalizeStatusCheckConfigurationForm(currentForm, scripts)
    );

    try {
      const stats: MachineStatsLatestResponse = await getLatestMachineStats(machineId, accessToken);
      setMachineStats(stats);
    } catch {
      setMachineStats(null);
    }
  }

  function handleScriptButtonConfigurationScriptChange(event: ChangeEvent<HTMLSelectElement>): void {
    const selectedScript: ScriptDefinitionResponse | undefined = machineScripts.find(
      (script: ScriptDefinitionResponse): boolean => script.id === event.target.value
    );
    setScriptButtonConfigurationForm((currentForm: ScriptButtonConfigurationFormState): ScriptButtonConfigurationFormState => ({
      ...currentForm,
      label: currentForm.label.trim() === "" ? selectedScript?.label ?? "" : currentForm.label,
      scriptDefinitionId: event.target.value,
      parameterValues: createDefaultScriptParameterValues(selectedScript?.parameterSchemaJson ?? "{}")
    }));
  }

  function handleStatusCheckConfigurationScriptChange(event: ChangeEvent<HTMLSelectElement>): void {
    const selectedScript: ScriptDefinitionResponse | undefined = machineScripts.find(
      (script: ScriptDefinitionResponse): boolean => script.id === event.target.value
    );
    setStatusCheckConfigurationForm((currentForm: StatusCheckConfigurationFormState): StatusCheckConfigurationFormState => ({
      ...currentForm,
      label: currentForm.label.trim() === "" ? selectedScript?.label ?? "" : currentForm.label,
      scriptDefinitionId: event.target.value,
      parameterValues: createDefaultScriptParameterValues(selectedScript?.parameterSchemaJson ?? "{}")
    }));
  }

  function handleScriptButtonConfigurationFieldChange(
    field: keyof Pick<ScriptButtonConfigurationFormState, "enabled" | "label" | "sortOrder">,
    value: string | boolean
  ): void {
    setScriptButtonConfigurationForm((currentForm: ScriptButtonConfigurationFormState): ScriptButtonConfigurationFormState => ({
      ...currentForm,
      [field]: value
    }));
  }

  function handleStatusCheckConfigurationFieldChange(
    field: keyof Pick<StatusCheckConfigurationFormState, "enabled" | "intervalSeconds" | "label" | "sortOrder">,
    value: string | boolean
  ): void {
    setStatusCheckConfigurationForm((currentForm: StatusCheckConfigurationFormState): StatusCheckConfigurationFormState => ({
      ...currentForm,
      [field]: value
    }));
  }

  function handleScriptButtonConfigurationParameterChange(name: string, value: string | boolean): void {
    setScriptButtonConfigurationForm((currentForm: ScriptButtonConfigurationFormState): ScriptButtonConfigurationFormState => ({
      ...currentForm,
      parameterValues: {
        ...currentForm.parameterValues,
        [name]: value
      }
    }));
  }

  function handleStatusCheckConfigurationParameterChange(name: string, value: string | boolean): void {
    setStatusCheckConfigurationForm((currentForm: StatusCheckConfigurationFormState): StatusCheckConfigurationFormState => ({
      ...currentForm,
      parameterValues: {
        ...currentForm.parameterValues,
        [name]: value
      }
    }));
  }

  function handleEditScriptButtonConfiguration(configuration: ScriptButtonConfigurationResponse): void {
    setScriptButtonConfigurationForm(createScriptButtonConfigurationForm(configuration));
  }

  function handleEditStatusCheckConfiguration(configuration: StatusCheckConfigurationResponse): void {
    setStatusCheckConfigurationForm(createStatusCheckConfigurationForm(configuration));
  }

  function handleCancelScriptButtonConfigurationEdit(): void {
    setScriptButtonConfigurationForm(normalizeScriptButtonConfigurationForm(initialScriptButtonConfigurationFormState, machineScripts));
  }

  function handleCancelStatusCheckConfigurationEdit(): void {
    setStatusCheckConfigurationForm(normalizeStatusCheckConfigurationForm(initialStatusCheckConfigurationFormState, machineScripts));
  }

  async function handleSaveScriptButtonConfiguration(): Promise<void> {
    if (session.accessToken === null || selectedMachine === null || scriptButtonConfigurationForm.scriptDefinitionId === "") {
      return;
    }

    try {
      const selectedScript: ScriptDefinitionResponse | undefined = machineScripts.find(
        (script: ScriptDefinitionResponse): boolean => script.id === scriptButtonConfigurationForm.scriptDefinitionId
      );
      const request: ScriptButtonConfigurationRequest = {
        scriptDefinitionId: scriptButtonConfigurationForm.scriptDefinitionId,
        label: scriptButtonConfigurationForm.label.trim() || selectedScript?.label || t("machines.scriptButton"),
        enabled: scriptButtonConfigurationForm.enabled,
        sortOrder: Number(scriptButtonConfigurationForm.sortOrder) || 0,
        parameters: buildScriptParameters(selectedScript?.parameterSchemaJson ?? "{}", scriptButtonConfigurationForm.parameterValues)
      };
      const savedConfiguration: ScriptButtonConfigurationResponse =
        scriptButtonConfigurationForm.configurationId === null
          ? await createScriptButtonConfiguration(selectedMachine.id, request, session.accessToken)
          : await updateScriptButtonConfiguration(
              selectedMachine.id,
              scriptButtonConfigurationForm.configurationId,
              request,
              session.accessToken
            );
      setScriptButtonConfigurations((currentConfigurations: ScriptButtonConfigurationResponse[]): ScriptButtonConfigurationResponse[] =>
        upsertScriptButtonConfiguration(currentConfigurations, savedConfiguration).sort(compareScriptButtonConfigurations)
      );
      const functions: MachineFunctionResponse[] = await listMachineFunctions(selectedMachine.id, session.accessToken);
      setMachineFunctions(functions);
      setScriptButtonConfigurationForm(normalizeScriptButtonConfigurationForm(initialScriptButtonConfigurationFormState, machineScripts));
      setNotice(t("machines.configurationSaved"));
      setError(null);
    } catch (saveError: unknown) {
      setError(getErrorMessage(saveError));
    }
  }

  async function handleSaveStatusCheckConfiguration(): Promise<void> {
    if (session.accessToken === null || selectedMachine === null || statusCheckConfigurationForm.scriptDefinitionId === "") {
      return;
    }

    try {
      const selectedScript: ScriptDefinitionResponse | undefined = machineScripts.find(
        (script: ScriptDefinitionResponse): boolean => script.id === statusCheckConfigurationForm.scriptDefinitionId
      );
      const request: StatusCheckConfigurationRequest = {
        scriptDefinitionId: statusCheckConfigurationForm.scriptDefinitionId,
        label: statusCheckConfigurationForm.label.trim() || selectedScript?.label || t("machines.statusCheck"),
        enabled: statusCheckConfigurationForm.enabled,
        intervalSeconds: Math.max(1, Number(statusCheckConfigurationForm.intervalSeconds) || 30),
        sortOrder: Number(statusCheckConfigurationForm.sortOrder) || 0,
        parameters: buildScriptParameters(selectedScript?.parameterSchemaJson ?? "{}", statusCheckConfigurationForm.parameterValues)
      };
      const savedConfiguration: StatusCheckConfigurationResponse =
        statusCheckConfigurationForm.configurationId === null
          ? await createStatusCheckConfiguration(selectedMachine.id, request, session.accessToken)
          : await updateStatusCheckConfiguration(
              selectedMachine.id,
              statusCheckConfigurationForm.configurationId,
              request,
              session.accessToken
            );
      setStatusCheckConfigurations((currentConfigurations: StatusCheckConfigurationResponse[]): StatusCheckConfigurationResponse[] =>
        upsertStatusCheckConfiguration(currentConfigurations, savedConfiguration).sort(compareStatusCheckConfigurations)
      );
      setStatusCheckConfigurationForm(normalizeStatusCheckConfigurationForm(initialStatusCheckConfigurationFormState, machineScripts));
      setNotice(t("machines.statusCheckSaved"));
      setError(null);
    } catch (saveError: unknown) {
      setError(getErrorMessage(saveError));
    }
  }

  async function handleDeleteScriptButtonConfiguration(configuration: ScriptButtonConfigurationResponse): Promise<void> {
    if (session.accessToken === null || selectedMachine === null || !window.confirm(t("machines.confirmDeleteConfiguration"))) {
      return;
    }

    try {
      await deleteScriptButtonConfiguration(selectedMachine.id, configuration.id, session.accessToken);
      setScriptButtonConfigurations((currentConfigurations: ScriptButtonConfigurationResponse[]): ScriptButtonConfigurationResponse[] =>
        currentConfigurations.filter((currentConfiguration: ScriptButtonConfigurationResponse): boolean => currentConfiguration.id !== configuration.id)
      );
      const functions: MachineFunctionResponse[] = await listMachineFunctions(selectedMachine.id, session.accessToken);
      setMachineFunctions(functions);
      setNotice(t("machines.configurationDeleted"));
      setError(null);
    } catch (deleteError: unknown) {
      setError(getErrorMessage(deleteError));
    }
  }

  async function handleDeleteStatusCheckConfiguration(configuration: StatusCheckConfigurationResponse): Promise<void> {
    if (session.accessToken === null || selectedMachine === null || !window.confirm(t("machines.confirmDeleteStatusCheck"))) {
      return;
    }

    try {
      await deleteStatusCheckConfiguration(selectedMachine.id, configuration.id, session.accessToken);
      setStatusCheckConfigurations((currentConfigurations: StatusCheckConfigurationResponse[]): StatusCheckConfigurationResponse[] =>
        currentConfigurations.filter((currentConfiguration: StatusCheckConfigurationResponse): boolean => currentConfiguration.id !== configuration.id)
      );
      setNotice(t("machines.statusCheckDeleted"));
      setError(null);
    } catch (deleteError: unknown) {
      setError(getErrorMessage(deleteError));
    }
  }

  async function handleAssignMachineFunction(functionType: MachineFunctionType, configurationId: string): Promise<void> {
    if (session.accessToken === null || selectedMachine === null || functionType === "WOL") {
      return;
    }

    try {
      const updatedFunction: MachineFunctionResponse = await assignMachineFunction(
        selectedMachine.id,
        functionType,
        { scriptConfigurationId: configurationId === "" ? null : configurationId },
        session.accessToken
      );
      setMachineFunctions((currentFunctions: MachineFunctionResponse[]): MachineFunctionResponse[] =>
        upsertMachineFunction(currentFunctions, updatedFunction)
      );
      setNotice(t("machines.functionAssigned"));
      setError(null);
    } catch (assignError: unknown) {
      setError(getErrorMessage(assignError));
    }
  }

  async function handleWakeOnLanInterfaceChange(interfaceId: string): Promise<void> {
    if (session.accessToken === null || selectedMachine === null) {
      return;
    }

    try {
      const request: UpdateWakeOnLanConfigurationRequest = {
        enabled: interfaceId !== "",
        primaryWolInterfaceId: interfaceId === "" ? null : interfaceId
      };
      const updatedMachine: MachineResponse = await updateWakeOnLanConfiguration(
        selectedMachine.id,
        request,
        session.accessToken
      );
      const functions: MachineFunctionResponse[] = await listMachineFunctions(selectedMachine.id, session.accessToken);
      setSelectedMachine(updatedMachine);
      setMachines((currentMachines: MachineResponse[]): MachineResponse[] =>
        currentMachines.map((machine: MachineResponse): MachineResponse =>
          machine.id === updatedMachine.id ? updatedMachine : machine
        )
      );
      setMachineFunctions(functions);
      setNotice(t("machines.wakeOnLanConfigured"));
      setError(null);
    } catch (configureError: unknown) {
      setError(getErrorMessage(configureError));
    }
  }

  function handleMachineRenameValueChange(event: ChangeEvent<HTMLInputElement>): void {
    setMachineRenameValue(event.target.value);
  }

  async function handleRenameMachine(): Promise<void> {
    if (session.accessToken === null || selectedMachine === null) {
      return;
    }

    try {
      const updatedMachine: MachineResponse = await renameMachine(
        selectedMachine.id,
        { displayName: machineRenameValue },
        session.accessToken
      );
      setSelectedMachine(updatedMachine);
      setMachineRenameValue(updatedMachine.displayName);
      setMachines((currentMachines: MachineResponse[]): MachineResponse[] => upsertMachine(currentMachines, updatedMachine));
      setNotice(t("machines.renamed"));
      setError(null);
    } catch (renameError: unknown) {
      setError(getErrorMessage(renameError));
    }
  }

  async function handleDeleteMachine(): Promise<void> {
    if (session.accessToken === null || selectedMachine === null || !window.confirm(t("machines.confirmDeleteMachine"))) {
      return;
    }

    setError(null);
    setNotice(null);

    try {
      if (selectedMachine.agent === null) {
        return;
      }

      await deleteAgent(selectedMachine.agent.id, session.accessToken);
      const removedMachineId: string = selectedMachine.id;
      setMachines((currentMachines: MachineResponse[]): MachineResponse[] =>
        currentMachines.filter((machine: MachineResponse): boolean => machine.id !== removedMachineId)
      );
      setSelectedMachine(null);
      setMachineRenameValue("");
      setMachineScripts([]);
      setMachineStats(null);
      setMachineStatusChecks([]);
      setMachineNetworkInterfaces([]);
      setMachineCommands([]);
      setMachineFunctions([]);
      setScriptButtonConfigurations([]);
      setScriptButtonConfigurationForm(initialScriptButtonConfigurationFormState);
      setStatusCheckConfigurations([]);
      setStatusCheckConfigurationForm(initialStatusCheckConfigurationFormState);
      setEnrollmentTokens(await listEnrollmentTokens(session.accessToken));
      setStatus(await getServerStatus(session.accessToken));
      setNotice(t("machines.machineDeleted"));
    } catch (deleteError: unknown) {
      setError(getErrorMessage(deleteError));
    }
  }

  async function handleExecuteScriptButtonConfiguration(configurationId: string): Promise<void> {
    if (session.accessToken === null || selectedMachine === null) {
      return;
    }

    try {
      const command: CommandResponse = await executeScriptButtonConfiguration(
        selectedMachine.id,
        configurationId,
        session.accessToken
      );
      setMachineCommands((currentCommands: CommandResponse[]): CommandResponse[] => [command, ...currentCommands]);
      setNotice(t("machines.commandSent"));
      setError(null);
    } catch (executeError: unknown) {
      setError(getErrorMessage(executeError));
    }
  }

  async function handleExecuteMachineFunction(functionType: MachineFunctionType): Promise<void> {
    if (session.accessToken === null || selectedMachine === null) {
      return;
    }

    try {
      const command: CommandResponse = await executeMachineFunction(selectedMachine.id, functionType, session.accessToken);
      setMachineCommands((currentCommands: CommandResponse[]): CommandResponse[] => [command, ...currentCommands]);
      setNotice(t("machines.commandSent"));
      setError(null);
    } catch (executeError: unknown) {
      setError(getErrorMessage(executeError));
    }
  }

  async function handleRefreshMachineStats(): Promise<void> {
    if (session.accessToken === null || selectedMachine === null) {
      return;
    }

    try {
      const command: CommandResponse = await refreshStatsCommand(selectedMachine.id, session.accessToken);
      setMachineCommands((currentCommands: CommandResponse[]): CommandResponse[] => [command, ...currentCommands]);
      setNotice(t("machines.commandSent"));
      setError(null);
    } catch (refreshError: unknown) {
      setError(getErrorMessage(refreshError));
    }
  }

  async function handleRefreshScriptManifest(): Promise<void> {
    if (session.accessToken === null || selectedMachine === null) {
      return;
    }

    try {
      const command: CommandResponse = await refreshScriptManifestCommand(selectedMachine.id, session.accessToken);
      setMachineCommands((currentCommands: CommandResponse[]): CommandResponse[] => [command, ...currentCommands]);
      setNotice(t("machines.commandSent"));
      setError(null);
    } catch (refreshError: unknown) {
      setError(getErrorMessage(refreshError));
    }
  }

  async function handleManagedUserRoleChange(userId: string, role: UserRole): Promise<void> {
    if (session.accessToken === null) {
      return;
    }

    setError(null);
    setNotice(null);
    setTemporaryPasswordNotice(null);

    try {
      const updatedUser: UserResponse = await updateUserRole(userId, { role }, session.accessToken);
      setUsers((currentUsers: UserResponse[]): UserResponse[] =>
        currentUsers.map((user: UserResponse): UserResponse => (user.id === updatedUser.id ? updatedUser : user))
      );
      if (session.user !== null && updatedUser.id === session.user.id) {
        setSession((currentSession: SessionState): SessionState => ({
          ...currentSession,
          user:
            currentSession.user === null
              ? null
              : {
                  ...currentSession.user,
                  role: updatedUser.role,
                  passwordChangeRequired: updatedUser.passwordChangeRequired
                }
        }));
        if (updatedUser.role !== "ADMIN") {
          setActiveView("dashboard");
        }
      }
      setNotice(t("admin.roleUpdated"));
    } catch (updateRoleError: unknown) {
      setError(getErrorMessage(updateRoleError));
    }
  }

  async function handleResetManagedUserPassword(user: UserResponse): Promise<void> {
    if (session.accessToken === null) {
      return;
    }

    setError(null);
    setNotice(null);
    setTemporaryPasswordNotice(null);

    try {
      const resetResponse: { user: UserResponse; temporaryPassword: string } = await resetUserPassword(
        user.id,
        session.accessToken
      );
      setUsers((currentUsers: UserResponse[]): UserResponse[] =>
        currentUsers.map((currentUser: UserResponse): UserResponse =>
          currentUser.id === resetResponse.user.id ? resetResponse.user : currentUser
        )
      );
      setTemporaryPasswordNotice({
        username: resetResponse.user.username,
        temporaryPassword: resetResponse.temporaryPassword
      });
      setNotice(t("admin.passwordReset"));
    } catch (resetPasswordError: unknown) {
      setError(getErrorMessage(resetPasswordError));
    }
  }

  async function handleDeleteManagedUser(user: UserResponse): Promise<void> {
    if (session.accessToken === null || !window.confirm(t("admin.confirmDelete"))) {
      return;
    }

    setError(null);
    setNotice(null);
    setTemporaryPasswordNotice(null);

    try {
      await deleteUser(user.id, session.accessToken);
      setUsers((currentUsers: UserResponse[]): UserResponse[] =>
        currentUsers.filter((currentUser: UserResponse): boolean => currentUser.id !== user.id)
      );
      setNotice(t("admin.userDeleted"));
    } catch (deleteUserError: unknown) {
      setError(getErrorMessage(deleteUserError));
    }
  }

  const mustChangePassword: boolean = passwordChangeRequired;

  return {
    activeView,
    canUseNavigation: session.user !== null && !mustChangePassword,
    createUserForm,
    enrollmentBundleNotice,
    enrollmentForm,
    enrollmentTokens,
    error,
    health,
    isAccountMenuOpen,
    isAdmin: session.user?.role === "ADMIN",
    isBootstrapping,
    isChangingPassword,
    isLoadingStatus,
    isSubmitting,
    language,
    loginForm,
    machineCommands,
    machineFunctions,
    machineNetworkInterfaces,
    machineRenameValue,
    machineScripts,
    machineStats,
    machineStatusChecks,
    machines,
    mustChangePassword,
    notice,
    passwordChangeForm,
    scriptButtonConfigurationForm,
    scriptButtonConfigurations,
    statusCheckConfigurationForm,
    statusCheckConfigurations,
    selectedMachine,
    session,
    shouldShowPasswordChangeForm: mustChangePassword || isPasswordChangeOpen,
    status,
    t,
    temporaryPasswordNotice,
    trustedCertificateForm,
    trustedCertificates,
    users,
    handleCancelPasswordChange,
    handleCancelTrustedCertificateEdit,
    handleConfirmPasswordChange,
    handleCreateUser,
    handleCreateUserRoleChange,
    handleCreateUserUsernameChange,
    handleCreateEnrollmentToken,
    handleCurrentPasswordChange,
    handleDeleteManagedUser,
    handleDeleteMachine,
    handleDeleteScriptButtonConfiguration,
    handleDeleteStatusCheckConfiguration,
    handleDeleteTrustedCertificate,
    handleEditScriptButtonConfiguration,
    handleEditStatusCheckConfiguration,
    handleEditTrustedCertificate,
    handleExecuteMachineFunction,
    handleExecuteScriptButtonConfiguration,
    handleLanguageChange,
    handleEnrollmentExpiresInChange,
    handleEnrollmentSuggestedNameChange,
    handleLogin,
    handleLoginPasswordChange,
    handleManagedUserRoleChange,
    handleMachineRenameValueChange,
    handleNavigateDashboard,
    handleNavigateEnrollment,
    handleNavigateMachines,
    handleNavigateUsers,
    handleNewPasswordChange,
    handleOpenPasswordChange,
    handlePasswordChangeSubmit,
    handleRefreshMachineStats,
    handleRefreshScriptManifest,
    handleRefreshStatus,
    handleRenameMachine,
    handleResetManagedUserPassword,
    handleAssignMachineFunction,
    handleCancelScriptButtonConfigurationEdit,
    handleCancelStatusCheckConfigurationEdit,
    handleSaveScriptButtonConfiguration,
    handleSaveStatusCheckConfiguration,
    handleScriptButtonConfigurationFieldChange,
    handleScriptButtonConfigurationParameterChange,
    handleScriptButtonConfigurationScriptChange,
    handleStatusCheckConfigurationFieldChange,
    handleStatusCheckConfigurationParameterChange,
    handleStatusCheckConfigurationScriptChange,
    handleTrustedCertificateFieldChange,
    handleTrustedCertificateSubmit,
    handleSelectMachine,
    handleToggleAccountMenu,
    handleWakeOnLanInterfaceChange,
    handleUsernameChange,
    handleLogout
  };
}

function getErrorMessage(error: unknown): string {
  if (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof (error as ApiError).message === "string"
  ) {
    return (error as ApiError).message;
  }

  return "Unexpected error";
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}

function createScriptButtonConfigurationForm(
  configuration: ScriptButtonConfigurationResponse
): ScriptButtonConfigurationFormState {
  return {
    configurationId: configuration.id,
    enabled: configuration.enabled,
    label: configuration.label,
    parameterValues: parseStoredParameterValues(configuration.parametersJson),
    scriptDefinitionId: configuration.scriptDefinitionId,
    sortOrder: String(configuration.sortOrder)
  };
}

function createStatusCheckConfigurationForm(
  configuration: StatusCheckConfigurationResponse
): StatusCheckConfigurationFormState {
  return {
    configurationId: configuration.id,
    enabled: configuration.enabled,
    intervalSeconds: String(configuration.intervalSeconds),
    label: configuration.label,
    parameterValues: parseStoredParameterValues(configuration.parametersJson),
    scriptDefinitionId: configuration.scriptDefinitionId,
    sortOrder: String(configuration.sortOrder)
  };
}

function normalizeScriptButtonConfigurationForm(
  form: ScriptButtonConfigurationFormState,
  scripts: ScriptDefinitionResponse[]
): ScriptButtonConfigurationFormState {
  const selectedScript: ScriptDefinitionResponse | undefined =
    scripts.find((script: ScriptDefinitionResponse): boolean => script.id === form.scriptDefinitionId) ?? scripts[0];

  if (selectedScript === undefined) {
    return {
      ...form,
      parameterValues: {},
      scriptDefinitionId: ""
    };
  }

  return {
    ...form,
    label: form.label.trim() === "" ? selectedScript.label : form.label,
    parameterValues:
      form.scriptDefinitionId === selectedScript.id
        ? form.parameterValues
        : createDefaultScriptParameterValues(selectedScript.parameterSchemaJson),
    scriptDefinitionId: selectedScript.id
  };
}

function normalizeStatusCheckConfigurationForm(
  form: StatusCheckConfigurationFormState,
  scripts: ScriptDefinitionResponse[]
): StatusCheckConfigurationFormState {
  const selectedScript: ScriptDefinitionResponse | undefined =
    scripts.find((script: ScriptDefinitionResponse): boolean => script.id === form.scriptDefinitionId) ?? scripts[0];

  if (selectedScript === undefined) {
    return {
      ...form,
      parameterValues: {},
      scriptDefinitionId: ""
    };
  }

  return {
    ...form,
    label: form.label.trim() === "" ? selectedScript.label : form.label,
    parameterValues:
      form.scriptDefinitionId === selectedScript.id
        ? form.parameterValues
        : createDefaultScriptParameterValues(selectedScript.parameterSchemaJson),
    scriptDefinitionId: selectedScript.id
  };
}

function parseStoredParameterValues(parametersJson: string): Record<string, string | boolean> {
  try {
    const parsedParameters: unknown = JSON.parse(parametersJson);
    if (typeof parsedParameters !== "object" || parsedParameters === null || Array.isArray(parsedParameters)) {
      return {};
    }

    return Object.entries(parsedParameters).reduce(
      (values: Record<string, string | boolean>, [name, value]: [string, unknown]): Record<string, string | boolean> => {
        if (typeof value === "boolean" || typeof value === "string") {
          return {
            ...values,
            [name]: value
          };
        }

        if (typeof value === "number") {
          return {
            ...values,
            [name]: String(value)
          };
        }

        return values;
      },
      {}
    );
  } catch {
    return {};
  }
}

function compareScriptButtonConfigurations(
  leftConfiguration: ScriptButtonConfigurationResponse,
  rightConfiguration: ScriptButtonConfigurationResponse
): number {
  return leftConfiguration.sortOrder - rightConfiguration.sortOrder || leftConfiguration.label.localeCompare(rightConfiguration.label);
}

function compareStatusCheckConfigurations(
  leftConfiguration: StatusCheckConfigurationResponse,
  rightConfiguration: StatusCheckConfigurationResponse
): number {
  return leftConfiguration.sortOrder - rightConfiguration.sortOrder || leftConfiguration.label.localeCompare(rightConfiguration.label);
}

function upsertScriptButtonConfiguration(
  configurations: ScriptButtonConfigurationResponse[],
  configuration: ScriptButtonConfigurationResponse
): ScriptButtonConfigurationResponse[] {
  if (configurations.some((currentConfiguration: ScriptButtonConfigurationResponse): boolean => currentConfiguration.id === configuration.id)) {
    return configurations.map((currentConfiguration: ScriptButtonConfigurationResponse): ScriptButtonConfigurationResponse =>
      currentConfiguration.id === configuration.id ? configuration : currentConfiguration
    );
  }

  return [...configurations, configuration];
}

function upsertStatusCheckConfiguration(
  configurations: StatusCheckConfigurationResponse[],
  configuration: StatusCheckConfigurationResponse
): StatusCheckConfigurationResponse[] {
  if (configurations.some((currentConfiguration: StatusCheckConfigurationResponse): boolean => currentConfiguration.id === configuration.id)) {
    return configurations.map((currentConfiguration: StatusCheckConfigurationResponse): StatusCheckConfigurationResponse =>
      currentConfiguration.id === configuration.id ? configuration : currentConfiguration
    );
  }

  return [...configurations, configuration];
}

function upsertMachine(machines: MachineResponse[], machine: MachineResponse): MachineResponse[] {
  if (machines.some((currentMachine: MachineResponse): boolean => currentMachine.id === machine.id)) {
    return machines.map((currentMachine: MachineResponse): MachineResponse =>
      currentMachine.id === machine.id ? machine : currentMachine
    );
  }

  return [...machines, machine];
}

function upsertMachineFunction(
  functions: MachineFunctionResponse[],
  machineFunction: MachineFunctionResponse
): MachineFunctionResponse[] {
  if (functions.some((currentFunction: MachineFunctionResponse): boolean => currentFunction.type === machineFunction.type)) {
    return functions.map((currentFunction: MachineFunctionResponse): MachineFunctionResponse =>
      currentFunction.type === machineFunction.type ? machineFunction : currentFunction
    );
  }

  return [...functions, machineFunction];
}

interface ScriptParameterDefinition {
  allowedValues: string[];
  defaultValue: string | boolean | null;
  name: string;
  type: string;
}

interface RawScriptParameterDefinition {
  allowedValues?: unknown;
  default?: unknown;
  type?: unknown;
}

function createDefaultScriptParameterValues(parameterSchemaJson: string): Record<string, string | boolean> {
  return parseScriptParameterSchema(parameterSchemaJson).reduce(
    (values: Record<string, string | boolean>, parameter: ScriptParameterDefinition): Record<string, string | boolean> => {
      if (parameter.defaultValue !== null) {
        return {
          ...values,
          [parameter.name]: parameter.defaultValue
        };
      }

      if ((parameter.type === "enum" || parameter.allowedValues.length > 0) && parameter.allowedValues[0] !== undefined) {
        return {
          ...values,
          [parameter.name]: parameter.allowedValues[0]
        };
      }

      if (parameter.type === "boolean" || parameter.type === "bool") {
        return {
          ...values,
          [parameter.name]: false
        };
      }

      return values;
    },
    {}
  );
}

function buildScriptParameters(
  parameterSchemaJson: string,
  values: Record<string, string | boolean>
): Record<string, unknown> {
  return parseScriptParameterSchema(parameterSchemaJson).reduce(
    (parameters: Record<string, unknown>, parameter: ScriptParameterDefinition): Record<string, unknown> => {
      const value: string | boolean | undefined = values[parameter.name];

      if (value === undefined || (typeof value === "string" && value.trim() === "")) {
        return parameters;
      }

      if (parameter.type === "boolean" || parameter.type === "bool") {
        return {
          ...parameters,
          [parameter.name]: Boolean(value)
        };
      }

      if (parameter.type === "number" || parameter.type === "integer") {
        const numericValue: number = Number(value);
        if (Number.isNaN(numericValue)) {
          return parameters;
        }

        return {
          ...parameters,
          [parameter.name]: parameter.type === "integer" ? Math.trunc(numericValue) : numericValue
        };
      }

      return {
        ...parameters,
        [parameter.name]: value
      };
    },
    {}
  );
}

function parseScriptParameterSchema(parameterSchemaJson: string): ScriptParameterDefinition[] {
  try {
    const schema: unknown = JSON.parse(parameterSchemaJson);
    if (typeof schema !== "object" || schema === null || Array.isArray(schema)) {
      return [];
    }

    return Object.entries(schema).map(([name, rawDefinition]: [string, unknown]): ScriptParameterDefinition => {
      const definition: RawScriptParameterDefinition =
        typeof rawDefinition === "object" && rawDefinition !== null && !Array.isArray(rawDefinition)
          ? (rawDefinition as RawScriptParameterDefinition)
          : {};
      const allowedValues: string[] = Array.isArray(definition.allowedValues)
        ? definition.allowedValues.map((value: unknown): string => String(value))
        : [];
      const type: string = typeof definition.type === "string" ? definition.type : "string";
      const defaultValue: string | boolean | null =
        typeof definition.default === "boolean" || typeof definition.default === "string"
          ? definition.default
          : definition.default === null || definition.default === undefined
            ? null
            : String(definition.default);

      return {
        allowedValues,
        defaultValue,
        name,
        type
      };
    });
  } catch {
    return [];
  }
}

function compareUsers(leftUser: UserResponse, rightUser: UserResponse): number {
  return leftUser.username.localeCompare(rightUser.username);
}

function compareTrustedCertificates(
  leftCertificate: TrustedCertificateResponse,
  rightCertificate: TrustedCertificateResponse
): number {
  return leftCertificate.alias.localeCompare(rightCertificate.alias);
}

function upsertTrustedCertificate(
  certificates: TrustedCertificateResponse[],
  certificate: TrustedCertificateResponse
): TrustedCertificateResponse[] {
  if (certificates.some((currentCertificate: TrustedCertificateResponse): boolean => currentCertificate.id === certificate.id)) {
    return certificates.map((currentCertificate: TrustedCertificateResponse): TrustedCertificateResponse =>
      currentCertificate.id === certificate.id ? certificate : currentCertificate
    );
  }

  return [...certificates, certificate];
}

function validatePasswordChangeForm(
  form: PasswordChangeFormState,
  username: string,
  t: (key: MessageKey) => string
): string | null {
  if (form.newPassword.length < 12) {
    return t("auth.passwordPolicy");
  }

  if (form.newPassword !== form.confirmPassword) {
    return t("auth.passwordMismatch");
  }

  if (form.newPassword === form.currentPassword) {
    return t("auth.passwordSameAsCurrent");
  }

  if (form.newPassword.toLocaleLowerCase() === username.toLocaleLowerCase()) {
    return t("auth.passwordSameAsUsername");
  }

  return null;
}
