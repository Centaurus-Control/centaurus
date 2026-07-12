export interface AuthTokenResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresAt: string;
}

export type UserRole = "ADMIN" | "OPERATOR" | "VIEWER";

export interface AuthenticatedUserResponse {
  id: string;
  username: string;
  role: UserRole;
  passwordChangeRequired: boolean;
}

export interface UserResponse {
  id: string;
  username: string;
  role: UserRole;
  passwordChangeRequired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ServerStatusResponse {
  application: string;
  status: string;
  timestamp: string;
  machineCount: number;
  agentCount: number;
  commandCount: number;
}

export type AgentCapabilityType = "STATS" | "SCRIPT_EXECUTION" | "WOL_RELAY";
export type AgentStatus = "REGISTERED" | "ONLINE" | "OFFLINE" | "REVOKED";
export type CommandStatus = "CREATED" | "SENT" | "ACCEPTED" | "FINISHED" | "FAILED" | "REJECTED" | "TIMEOUT";
export type CommandType = "EXECUTE_SCRIPT" | "SEND_WOL" | "REFRESH_STATS" | "REFRESH_SCRIPT_MANIFEST";
export type IpAddressFamily = "IPV4" | "IPV6";
export type MachineStatus = "UNKNOWN" | "ONLINE" | "OFFLINE";
export type MachineFunctionType = "WOL" | "REBOOT" | "SHUTDOWN";

export interface AgentSummaryResponse {
  id: string;
  installationId: string;
  displayName: string;
  hostname: string;
  agentVersion: string;
  status: AgentStatus;
  lastConnectedAt: string | null;
  lastSeenAt: string | null;
  capabilities: AgentCapabilityType[];
  createdAt: string;
  updatedAt: string;
}

export interface MachineResponse {
  id: string;
  displayName: string;
  hostname: string;
  status: MachineStatus;
  lastSeenAt: string | null;
  wolEnabled: boolean;
  primaryWolInterfaceId: string | null;
  agent: AgentSummaryResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface MachineStatusChangedEvent {
  machineId: string;
  status: MachineStatus;
  changedAt: string;
}

export interface ScriptDefinitionResponse {
  id: string;
  agentId: string;
  scriptId: string;
  label: string;
  description: string | null;
  manifestVersion: number;
  parameterSchemaJson: string;
  resultSchemaJson: string;
  updatedAt: string;
}

export interface MachineStatsLatestResponse {
  machineId: string;
  agentId: string;
  cpuLoad: number;
  memoryUsedPercent: number;
  uptimeSeconds: number;
  updatedAt: string;
}

export interface MachineStatusCheckLatestResponse {
  id: string;
  machineId: string;
  agentId: string;
  checkId: string;
  label: string;
  healthy: boolean | null;
  exitCode: number | null;
  stdout: string | null;
  stderr: string | null;
  error: string | null;
  sortOrder: number;
  checkedAt: string;
  updatedAt: string;
}

export interface StatusCheckConfigurationResponse {
  id: string;
  machineId: string;
  scriptDefinitionId: string;
  agentId: string;
  scriptId: string;
  label: string;
  enabled: boolean;
  intervalSeconds: number;
  sortOrder: number;
  parametersJson: string;
  createdAt: string;
  updatedAt: string;
}

export interface StatusCheckConfigurationRequest {
  scriptDefinitionId: string;
  label: string;
  enabled: boolean;
  intervalSeconds: number;
  sortOrder: number;
  parameters: Record<string, unknown>;
}

export interface MachineNetworkInterfaceResponse {
  id: string;
  machineId: string;
  agentId: string;
  interfaceName: string;
  displayName: string | null;
  macAddress: string | null;
  ipAddress: string | null;
  prefixLength: number | null;
  family: IpAddressFamily;
  up: boolean;
  loopback: boolean;
  virtual: boolean;
  wireless: boolean;
  defaultRoute: boolean;
  wolCandidate: boolean;
  lastSeenAt: string;
}

export interface CommandResponse {
  id: string;
  commandId: string;
  machineId: string | null;
  agentId: string | null;
  commandType: CommandType;
  status: CommandStatus;
  createdByUserId: string | null;
  createdAt: string;
  sentAt: string | null;
  acceptedAt: string | null;
  finishedAt: string | null;
  payloadJson: string;
  resultJson: string | null;
  errorJson: string | null;
}

export interface ExecuteScriptRequest {
  scriptId: string;
  parameters: Record<string, unknown>;
}

export interface SendWakeOnLanRequest {
  macAddress: string;
  broadcastAddress: string;
  port: number | null;
}

export interface UpdateWakeOnLanConfigurationRequest {
  enabled: boolean;
  primaryWolInterfaceId: string | null;
}

export interface RenameMachineRequest {
  displayName: string;
}

export interface ScriptButtonConfigurationResponse {
  id: string;
  machineId: string;
  scriptDefinitionId: string;
  agentId: string;
  scriptId: string;
  label: string;
  enabled: boolean;
  sortOrder: number;
  parametersJson: string;
  createdAt: string;
  updatedAt: string;
}

export interface ScriptButtonConfigurationRequest {
  scriptDefinitionId: string;
  label: string;
  enabled: boolean;
  sortOrder: number;
  parameters: Record<string, unknown>;
}

export interface MachineFunctionResponse {
  type: MachineFunctionType;
  enabled: boolean;
  scriptConfiguration: ScriptButtonConfigurationResponse | null;
}

export interface MachineFunctionAssignmentRequest {
  scriptConfigurationId: string | null;
}

export interface ActuatorHealthResponse {
  status: string;
  components?: Record<string, unknown>;
  groups?: string[];
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ChangePasswordResponse {
  passwordChangeRequired: boolean;
  changedAt: string;
  revokedSessionCount: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface CreateUserRequest {
  username: string;
  role: UserRole;
}

export interface CreateUserResponse {
  user: UserResponse;
  temporaryPassword: string;
}

export interface UpdateUserRoleRequest {
  role: UserRole;
}

export interface ResetUserPasswordResponse {
  user: UserResponse;
  temporaryPassword: string;
}

export interface TrustedCertificateResponse {
  id: string;
  alias: string;
  displayName: string;
  certificatePem: string;
  enabled: boolean;
  subjectDn: string;
  issuerDn: string;
  serialNumber: string;
  notBefore: string;
  notAfter: string;
  sha256Fingerprint: string;
  createdAt: string;
  updatedAt: string;
}

export interface TrustedCertificateRequest {
  alias: string;
  displayName: string;
  certificatePem: string;
  enabled: boolean;
}

export interface EnrollmentTokenResponse {
  id: string;
  suggestedName: string | null;
  expiresAt: string;
  usedAt: string | null;
  usedByAgentId: string | null;
  createdAt: string;
}

export interface CreateEnrollmentTokenRequest {
  suggestedName: string | null;
  expiresIn: string;
}

export interface CreateEnrollmentTokenResponse {
  token: EnrollmentTokenResponse;
  enrollmentBundle: string;
}

export interface ApiError {
  message: string;
  status: number;
}

async function readError(response: Response): Promise<ApiError> {
  const fallback: ApiError = {
    message: response.statusText || "Request failed",
    status: response.status
  };

  try {
    const body: unknown = await response.json();
    if (
      typeof body === "object" &&
      body !== null &&
      "detail" in body &&
      typeof body.detail === "string"
    ) {
      return {
        message: body.detail,
        status: response.status
      };
    }

    if (
      typeof body === "object" &&
      body !== null &&
      "title" in body &&
      typeof body.title === "string"
    ) {
      return {
        message: body.title,
        status: response.status
      };
    }

    if (
      typeof body === "object" &&
      body !== null &&
      "message" in body &&
      typeof body.message === "string"
    ) {
      return {
        message: body.message,
        status: response.status
      };
    }
  } catch {
    return fallback;
  }

  return fallback;
}

async function requestJson<TResponse>(
  path: string,
  init: RequestInit,
  accessToken: string | null
): Promise<TResponse> {
  const headers: Headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (accessToken !== null) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response: Response = await fetch(path, {
    ...init,
    headers,
    credentials: "include"
  });

  if (!response.ok) {
    throw await readError(response);
  }

  return (await response.json()) as TResponse;
}

interface ParsedServerSentEvent {
  data: string;
  eventName: string;
}

function parseServerSentEvent(rawEvent: string): ParsedServerSentEvent | null {
  const lines: string[] = rawEvent.split(/\r?\n/);
  const dataLines: string[] = [];
  let eventName: string = "message";

  for (const line of lines) {
    if (line === "" || line.startsWith(":")) {
      continue;
    }

    const separatorIndex: number = line.indexOf(":");
    const field: string = separatorIndex === -1 ? line : line.slice(0, separatorIndex);
    const value: string = separatorIndex === -1 ? "" : line.slice(separatorIndex + 1).replace(/^ /, "");

    if (field === "event") {
      eventName = value;
    }

    if (field === "data") {
      dataLines.push(value);
    }
  }

  if (dataLines.length === 0) {
    return null;
  }

  return {
    data: dataLines.join("\n"),
    eventName
  };
}

export async function login(request: LoginRequest): Promise<AuthTokenResponse> {
  return requestJson<AuthTokenResponse>(
    "/api/auth/login",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    null
  );
}

export async function refreshAccessToken(): Promise<AuthTokenResponse> {
  return requestJson<AuthTokenResponse>(
    "/api/auth/refresh",
    {
      method: "POST"
    },
    null
  );
}

export async function logout(accessToken: string | null): Promise<void> {
  const response: Response = await fetch("/api/auth/logout", {
    method: "POST",
    credentials: "include",
    headers:
      accessToken === null
        ? undefined
        : {
            Authorization: `Bearer ${accessToken}`
          }
  });

  if (!response.ok) {
    throw await readError(response);
  }
}

export async function changePassword(
  request: ChangePasswordRequest,
  accessToken: string
): Promise<ChangePasswordResponse> {
  return requestJson<ChangePasswordResponse>(
    "/api/auth/change-password",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function getCurrentUser(accessToken: string): Promise<AuthenticatedUserResponse> {
  return requestJson<AuthenticatedUserResponse>(
    "/api/me",
    {
      method: "GET"
    },
    accessToken
  );
}

export async function getServerStatus(accessToken: string): Promise<ServerStatusResponse> {
  return requestJson<ServerStatusResponse>(
    "/api/server/status",
    {
      method: "GET"
    },
    accessToken
  );
}

export async function listUsers(accessToken: string): Promise<UserResponse[]> {
  return requestJson<UserResponse[]>(
    "/api/admin/users",
    {
      method: "GET"
    },
    accessToken
  );
}

export async function createUser(
  request: CreateUserRequest,
  accessToken: string
): Promise<CreateUserResponse> {
  return requestJson<CreateUserResponse>(
    "/api/admin/users",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function updateUserRole(
  userId: string,
  request: UpdateUserRoleRequest,
  accessToken: string
): Promise<UserResponse> {
  return requestJson<UserResponse>(
    `/api/admin/users/${userId}/role`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function resetUserPassword(
  userId: string,
  accessToken: string
): Promise<ResetUserPasswordResponse> {
  return requestJson<ResetUserPasswordResponse>(
    `/api/admin/users/${userId}/reset-password`,
    {
      method: "POST"
    },
    accessToken
  );
}

export async function deleteUser(userId: string, accessToken: string): Promise<void> {
  const response: Response = await fetch(`/api/admin/users/${userId}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw await readError(response);
  }
}

export async function listTrustedCertificates(accessToken: string): Promise<TrustedCertificateResponse[]> {
  return requestJson<TrustedCertificateResponse[]>(
    "/api/admin/trusted-certificates",
    {
      method: "GET"
    },
    accessToken
  );
}

export async function createTrustedCertificate(
  request: TrustedCertificateRequest,
  accessToken: string
): Promise<TrustedCertificateResponse> {
  return requestJson<TrustedCertificateResponse>(
    "/api/admin/trusted-certificates",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function updateTrustedCertificate(
  certificateId: string,
  request: TrustedCertificateRequest,
  accessToken: string
): Promise<TrustedCertificateResponse> {
  return requestJson<TrustedCertificateResponse>(
    `/api/admin/trusted-certificates/${certificateId}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function deleteTrustedCertificate(certificateId: string, accessToken: string): Promise<void> {
  const response: Response = await fetch(`/api/admin/trusted-certificates/${certificateId}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw await readError(response);
  }
}

export async function listEnrollmentTokens(accessToken: string): Promise<EnrollmentTokenResponse[]> {
  return requestJson<EnrollmentTokenResponse[]>(
    "/api/admin/enrollment-tokens",
    {
      method: "GET"
    },
    accessToken
  );
}

export async function createEnrollmentToken(
  request: CreateEnrollmentTokenRequest,
  accessToken: string
): Promise<CreateEnrollmentTokenResponse> {
  return requestJson<CreateEnrollmentTokenResponse>(
    "/api/admin/enrollment-tokens",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function listMachines(accessToken: string): Promise<MachineResponse[]> {
  return requestJson<MachineResponse[]>("/api/machines", { method: "GET" }, accessToken);
}

export async function getMachine(machineId: string, accessToken: string): Promise<MachineResponse> {
  return requestJson<MachineResponse>(`/api/machines/${machineId}`, { method: "GET" }, accessToken);
}

export async function renameMachine(
  machineId: string,
  request: RenameMachineRequest,
  accessToken: string
): Promise<MachineResponse> {
  return requestJson<MachineResponse>(
    `/api/admin/machines/${machineId}/rename`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function deleteAgent(agentId: string, accessToken: string): Promise<void> {
  const response: Response = await fetch(`/api/admin/agents/${agentId}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw await readError(response);
  }
}

export async function streamMachineEvents(
  accessToken: string,
  signal: AbortSignal,
  onMachineStatusChanged: (event: MachineStatusChangedEvent) => void
): Promise<void> {
  const response: Response = await fetch("/api/machines/events", {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
      Authorization: `Bearer ${accessToken}`
    },
    credentials: "include",
    signal
  });

  if (!response.ok) {
    throw await readError(response);
  }

  if (response.body === null) {
    throw {
      message: "Machine event stream is not readable",
      status: response.status
    } satisfies ApiError;
  }

  const reader: ReadableStreamDefaultReader<Uint8Array> = response.body.getReader();
  const decoder: TextDecoder = new TextDecoder();
  let buffer: string = "";

  while (!signal.aborted) {
    const result: ReadableStreamReadResult<Uint8Array> = await reader.read();
    if (result.done) {
      break;
    }

    buffer += decoder.decode(result.value, { stream: true });
    const events: string[] = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() ?? "";

    for (const rawEvent of events) {
      const event: ParsedServerSentEvent | null = parseServerSentEvent(rawEvent);
      if (event?.eventName === "machine-status-changed") {
        onMachineStatusChanged(JSON.parse(event.data) as MachineStatusChangedEvent);
      }
    }
  }
}

export async function listMachineScripts(machineId: string, accessToken: string): Promise<ScriptDefinitionResponse[]> {
  return requestJson<ScriptDefinitionResponse[]>(`/api/machines/${machineId}/scripts`, { method: "GET" }, accessToken);
}

export async function getLatestMachineStats(
  machineId: string,
  accessToken: string
): Promise<MachineStatsLatestResponse> {
  return requestJson<MachineStatsLatestResponse>(`/api/machines/${machineId}/stats/latest`, { method: "GET" }, accessToken);
}

export async function listLatestMachineStatusChecks(
  machineId: string,
  accessToken: string
): Promise<MachineStatusCheckLatestResponse[]> {
  return requestJson<MachineStatusCheckLatestResponse[]>(
    `/api/machines/${machineId}/status-checks/latest`,
    { method: "GET" },
    accessToken
  );
}

export async function listMachineNetworkInterfaces(
  machineId: string,
  accessToken: string
): Promise<MachineNetworkInterfaceResponse[]> {
  return requestJson<MachineNetworkInterfaceResponse[]>(
    `/api/machines/${machineId}/network-interfaces`,
    { method: "GET" },
    accessToken
  );
}

export async function listMachineCommands(machineId: string, accessToken: string): Promise<CommandResponse[]> {
  return requestJson<CommandResponse[]>(`/api/machines/${machineId}/commands`, { method: "GET" }, accessToken);
}

export async function listMachineFunctions(machineId: string, accessToken: string): Promise<MachineFunctionResponse[]> {
  return requestJson<MachineFunctionResponse[]>(`/api/machines/${machineId}/functions`, { method: "GET" }, accessToken);
}

export async function updateWakeOnLanConfiguration(
  machineId: string,
  request: UpdateWakeOnLanConfigurationRequest,
  accessToken: string
): Promise<MachineResponse> {
  return requestJson<MachineResponse>(
    `/api/machines/${machineId}/wake-on-lan`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function listScriptButtonConfigurations(
  machineId: string,
  accessToken: string
): Promise<ScriptButtonConfigurationResponse[]> {
  return requestJson<ScriptButtonConfigurationResponse[]>(
    `/api/machines/${machineId}/script-configurations`,
    { method: "GET" },
    accessToken
  );
}

export async function listStatusCheckConfigurations(
  machineId: string,
  accessToken: string
): Promise<StatusCheckConfigurationResponse[]> {
  return requestJson<StatusCheckConfigurationResponse[]>(
    `/api/machines/${machineId}/status-check-configurations`,
    { method: "GET" },
    accessToken
  );
}

export async function createStatusCheckConfiguration(
  machineId: string,
  request: StatusCheckConfigurationRequest,
  accessToken: string
): Promise<StatusCheckConfigurationResponse> {
  return requestJson<StatusCheckConfigurationResponse>(
    `/api/admin/machines/${machineId}/status-check-configurations`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function updateStatusCheckConfiguration(
  machineId: string,
  configurationId: string,
  request: StatusCheckConfigurationRequest,
  accessToken: string
): Promise<StatusCheckConfigurationResponse> {
  return requestJson<StatusCheckConfigurationResponse>(
    `/api/admin/machines/${machineId}/status-check-configurations/${configurationId}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function deleteStatusCheckConfiguration(
  machineId: string,
  configurationId: string,
  accessToken: string
): Promise<void> {
  const response: Response = await fetch(`/api/admin/machines/${machineId}/status-check-configurations/${configurationId}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw await readError(response);
  }
}

export async function listCommands(accessToken: string): Promise<CommandResponse[]> {
  return requestJson<CommandResponse[]>("/api/commands", { method: "GET" }, accessToken);
}

export async function executeScriptCommand(
  machineId: string,
  request: ExecuteScriptRequest,
  accessToken: string
): Promise<CommandResponse> {
  return requestJson<CommandResponse>(
    `/api/machines/${machineId}/commands/execute-script`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function createScriptButtonConfiguration(
  machineId: string,
  request: ScriptButtonConfigurationRequest,
  accessToken: string
): Promise<ScriptButtonConfigurationResponse> {
  return requestJson<ScriptButtonConfigurationResponse>(
    `/api/admin/machines/${machineId}/script-configurations`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function updateScriptButtonConfiguration(
  machineId: string,
  configurationId: string,
  request: ScriptButtonConfigurationRequest,
  accessToken: string
): Promise<ScriptButtonConfigurationResponse> {
  return requestJson<ScriptButtonConfigurationResponse>(
    `/api/admin/machines/${machineId}/script-configurations/${configurationId}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function deleteScriptButtonConfiguration(
  machineId: string,
  configurationId: string,
  accessToken: string
): Promise<void> {
  const response: Response = await fetch(`/api/admin/machines/${machineId}/script-configurations/${configurationId}`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw await readError(response);
  }
}

export async function assignMachineFunction(
  machineId: string,
  functionType: MachineFunctionType,
  request: MachineFunctionAssignmentRequest,
  accessToken: string
): Promise<MachineFunctionResponse> {
  return requestJson<MachineFunctionResponse>(
    `/api/admin/machines/${machineId}/functions/${functionType}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function executeScriptButtonConfiguration(
  machineId: string,
  configurationId: string,
  accessToken: string
): Promise<CommandResponse> {
  return requestJson<CommandResponse>(
    `/api/machines/${machineId}/script-configurations/${configurationId}/execute`,
    { method: "POST" },
    accessToken
  );
}

export async function executeMachineFunction(
  machineId: string,
  functionType: MachineFunctionType,
  accessToken: string
): Promise<CommandResponse> {
  return requestJson<CommandResponse>(
    `/api/machines/${machineId}/functions/${functionType}/execute`,
    { method: "POST" },
    accessToken
  );
}

export async function sendWakeOnLanCommand(
  machineId: string,
  request: SendWakeOnLanRequest,
  accessToken: string
): Promise<CommandResponse> {
  return requestJson<CommandResponse>(
    `/api/machines/${machineId}/commands/wake-on-lan`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    },
    accessToken
  );
}

export async function refreshStatsCommand(machineId: string, accessToken: string): Promise<CommandResponse> {
  return requestJson<CommandResponse>(
    `/api/machines/${machineId}/commands/refresh-stats`,
    { method: "POST" },
    accessToken
  );
}

export async function refreshScriptManifestCommand(machineId: string, accessToken: string): Promise<CommandResponse> {
  return requestJson<CommandResponse>(
    `/api/machines/${machineId}/commands/refresh-script-manifest`,
    { method: "POST" },
    accessToken
  );
}

export async function getActuatorHealth(): Promise<ActuatorHealthResponse> {
  return requestJson<ActuatorHealthResponse>(
    "/actuator/health",
    {
      method: "GET"
    },
    null
  );
}
