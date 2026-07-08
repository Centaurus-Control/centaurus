import { Monitor, Pencil, Play, Power, RefreshCw, RotateCw, Save, Square, Trash2, X } from "lucide-react";
import { type ChangeEvent, type ReactElement, useState } from "react";
import {
  type CommandResponse,
  type MachineFunctionResponse,
  type MachineFunctionType,
  type MachineNetworkInterfaceResponse,
  type MachineResponse,
  type MachineStatsLatestResponse,
  type MachineStatusCheckLatestResponse,
  type ScriptButtonConfigurationResponse,
  type ScriptDefinitionResponse,
  type StatusCheckConfigurationResponse,
  type UserRole
} from "../api/client";
import { type LanguageCode, type MessageKey } from "../i18n/messages";

export interface ScriptButtonConfigurationFormState {
  configurationId: string | null;
  enabled: boolean;
  label: string;
  parameterValues: Record<string, string | boolean>;
  scriptDefinitionId: string;
  sortOrder: string;
}

export interface StatusCheckConfigurationFormState {
  configurationId: string | null;
  enabled: boolean;
  intervalSeconds: string;
  label: string;
  parameterValues: Record<string, string | boolean>;
  scriptDefinitionId: string;
  sortOrder: string;
}

export interface MachinesPageProps {
  commands: CommandResponse[];
  functions: MachineFunctionResponse[];
  language: LanguageCode;
  machines: MachineResponse[];
  networkInterfaces: MachineNetworkInterfaceResponse[];
  role: UserRole;
  machineRenameValue: string;
  scriptButtonConfigurationForm: ScriptButtonConfigurationFormState;
  scriptButtonConfigurations: ScriptButtonConfigurationResponse[];
  scripts: ScriptDefinitionResponse[];
  selectedMachine: MachineResponse | null;
  stats: MachineStatsLatestResponse | null;
  statusChecks: MachineStatusCheckLatestResponse[];
  statusCheckConfigurationForm: StatusCheckConfigurationFormState;
  statusCheckConfigurations: StatusCheckConfigurationResponse[];
  t: (key: MessageKey) => string;
  onAssignMachineFunction: (functionType: MachineFunctionType, configurationId: string) => Promise<void>;
  onCancelScriptButtonConfigurationEdit: () => void;
  onCancelStatusCheckConfigurationEdit: () => void;
  onDeleteScriptButtonConfiguration: (configuration: ScriptButtonConfigurationResponse) => Promise<void>;
  onDeleteStatusCheckConfiguration: (configuration: StatusCheckConfigurationResponse) => Promise<void>;
  onDeleteMachine: () => Promise<void>;
  onEditScriptButtonConfiguration: (configuration: ScriptButtonConfigurationResponse) => void;
  onEditStatusCheckConfiguration: (configuration: StatusCheckConfigurationResponse) => void;
  onExecuteMachineFunction: (functionType: MachineFunctionType) => Promise<void>;
  onExecuteScriptButtonConfiguration: (configurationId: string) => Promise<void>;
  onMachineRenameValueChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onRefreshScriptManifest: () => Promise<void>;
  onRefreshStats: () => Promise<void>;
  onRenameMachine: () => Promise<void>;
  onSaveScriptButtonConfiguration: () => Promise<void>;
  onSaveStatusCheckConfiguration: () => Promise<void>;
  onScriptButtonConfigurationFieldChange: (
    field: keyof Pick<ScriptButtonConfigurationFormState, "enabled" | "label" | "sortOrder">,
    value: string | boolean
  ) => void;
  onScriptButtonConfigurationParameterChange: (name: string, value: string | boolean) => void;
  onScriptButtonConfigurationScriptChange: (event: ChangeEvent<HTMLSelectElement>) => void;
  onStatusCheckConfigurationFieldChange: (
    field: keyof Pick<StatusCheckConfigurationFormState, "enabled" | "intervalSeconds" | "label" | "sortOrder">,
    value: string | boolean
  ) => void;
  onStatusCheckConfigurationParameterChange: (name: string, value: string | boolean) => void;
  onStatusCheckConfigurationScriptChange: (event: ChangeEvent<HTMLSelectElement>) => void;
  onSelectMachine: (machineId: string) => Promise<void>;
  onWakeOnLanInterfaceChange: (interfaceId: string) => Promise<void>;
}

export function MachinesPage({
  commands,
  functions,
  language,
  machines,
  networkInterfaces,
  role,
  machineRenameValue,
  scriptButtonConfigurationForm,
  scriptButtonConfigurations,
  scripts,
  selectedMachine,
  stats,
  statusChecks,
  statusCheckConfigurationForm,
  statusCheckConfigurations,
  t,
  onAssignMachineFunction,
  onCancelScriptButtonConfigurationEdit,
  onCancelStatusCheckConfigurationEdit,
  onDeleteScriptButtonConfiguration,
  onDeleteStatusCheckConfiguration,
  onDeleteMachine,
  onEditScriptButtonConfiguration,
  onEditStatusCheckConfiguration,
  onExecuteMachineFunction,
  onExecuteScriptButtonConfiguration,
  onMachineRenameValueChange,
  onRefreshScriptManifest,
  onRefreshStats,
  onRenameMachine,
  onSaveScriptButtonConfiguration,
  onSaveStatusCheckConfiguration,
  onScriptButtonConfigurationFieldChange,
  onScriptButtonConfigurationParameterChange,
  onScriptButtonConfigurationScriptChange,
  onStatusCheckConfigurationFieldChange,
  onStatusCheckConfigurationParameterChange,
  onStatusCheckConfigurationScriptChange,
  onSelectMachine,
  onWakeOnLanInterfaceChange
}: MachinesPageProps): ReactElement {
  const [machineView, setMachineView] = useState<"overview" | "config">("overview");
  const canExecuteCommands: boolean = role === "ADMIN" || role === "OPERATOR";
  const isAdmin: boolean = role === "ADMIN";
  const activeMachineView: "overview" | "config" = isAdmin ? machineView : "overview";
  const machineOnline: boolean =
    selectedMachine?.status === "ONLINE" || selectedMachine?.agent?.status === "ONLINE";
  const selectedScript: ScriptDefinitionResponse | null =
    scripts.find(
      (script: ScriptDefinitionResponse): boolean => script.id === scriptButtonConfigurationForm.scriptDefinitionId
    ) ?? null;
  const selectedStatusCheckScript: ScriptDefinitionResponse | null =
    scripts.find(
      (script: ScriptDefinitionResponse): boolean => script.id === statusCheckConfigurationForm.scriptDefinitionId
    ) ?? null;
  const parameterDefinitions: ScriptParameterDefinition[] = parseParameterSchema(selectedScript?.parameterSchemaJson ?? "{}");
  const statusCheckParameterDefinitions: ScriptParameterDefinition[] = parseParameterSchema(selectedStatusCheckScript?.parameterSchemaJson ?? "{}");
  const fixedFunctionConfigurationIds: Set<string> = new Set(
    functions
      .filter((machineFunction: MachineFunctionResponse): boolean => machineFunction.type === "REBOOT" || machineFunction.type === "SHUTDOWN")
      .map((machineFunction: MachineFunctionResponse): string | null => machineFunction.scriptConfiguration?.id ?? null)
      .filter((configurationId: string | null): configurationId is string => configurationId !== null)
  );
  const enabledConfigurations: ScriptButtonConfigurationResponse[] = scriptButtonConfigurations.filter(
    (configuration: ScriptButtonConfigurationResponse): boolean =>
      configuration.enabled && !fixedFunctionConfigurationIds.has(configuration.id)
  );
  const wakeOnLanInterfaces: MachineNetworkInterfaceResponse[] = networkInterfaces.filter(
    (networkInterface: MachineNetworkInterfaceResponse): boolean => networkInterface.macAddress !== null
  );
  const latestStatusChecksByConfigurationId: Map<string, MachineStatusCheckLatestResponse> = new Map(
    statusChecks.map((statusCheck: MachineStatusCheckLatestResponse): [string, MachineStatusCheckLatestResponse] => [
      statusCheck.checkId,
      statusCheck
    ])
  );
  const displayedStatusChecks: DisplayedStatusCheck[] =
    statusCheckConfigurations.length === 0
      ? statusChecks.map((statusCheck: MachineStatusCheckLatestResponse): DisplayedStatusCheck => ({
          checkedAt: statusCheck.checkedAt,
          healthy: machineOnline ? statusCheck.healthy : null,
          id: statusCheck.id,
          label: statusCheck.label,
          title: statusCheck.error ?? statusCheck.stderr ?? statusCheck.stdout ?? ""
        }))
      : statusCheckConfigurations
          .filter((configuration: StatusCheckConfigurationResponse): boolean => configuration.enabled)
          .map((configuration: StatusCheckConfigurationResponse): DisplayedStatusCheck => {
            const latestStatusCheck: MachineStatusCheckLatestResponse | undefined = latestStatusChecksByConfigurationId.get(configuration.id);
            return {
              checkedAt: latestStatusCheck?.checkedAt ?? null,
              healthy: machineOnline ? latestStatusCheck?.healthy ?? null : null,
              id: configuration.id,
              label: configuration.label,
              title: latestStatusCheck?.error ?? latestStatusCheck?.stderr ?? latestStatusCheck?.stdout ?? ""
            };
          });

  return (
    <section className="machinesLayout">
      <aside className="panel machineListPanel">
        <div className="panelTitle">
          <Monitor aria-hidden="true" size={22} />
          <span>{t("machines.title")}</span>
        </div>
        {machines.length === 0 ? (
          <p className="hintText">{t("machines.noMachines")}</p>
        ) : (
          <div className="machineList">
            {machines.map((machine: MachineResponse): ReactElement => (
              <button
                className={machineListItemClassName(machine, selectedMachine?.id === machine.id)}
                key={machine.id}
                type="button"
                onClick={() => {
                  void onSelectMachine(machine.id);
                }}
              >
                <strong>{machine.displayName}</strong>
                <span>{machine.hostname}</span>
                <small className="machineStatusLabel">{machine.status}</small>
              </button>
            ))}
          </div>
        )}
      </aside>

      {selectedMachine === null ? null : (
        <section className="machineDetail">
          <article className="panel statusPanel machineSummaryPanel">
            <div className="machineSummaryDetails">
              <div className="panelTitle">
                <Monitor aria-hidden="true" size={22} />
                <span>{selectedMachine.displayName}</span>
              </div>
              <dl>
                <div>
                  <dt>{t("machines.hostname")}</dt>
                  <dd>{selectedMachine.hostname}</dd>
                </div>
                <div>
                  <dt>{t("machines.status")}</dt>
                  <dd>{selectedMachine.status}</dd>
                </div>
                <div>
                  <dt>{t("machines.lastSeen")}</dt>
                  <dd>{formatNullableDateTime(selectedMachine.lastSeenAt, language)}</dd>
                </div>
              </dl>
            </div>
            {isAdmin ? (
              <button
                className="dangerActionButton iconTextButton"
                disabled={selectedMachine.agent === null}
                type="button"
                onClick={() => void onDeleteMachine()}
              >
                <Trash2 aria-hidden="true" size={18} />
                <span>{t("machines.deleteMachine")}</span>
              </button>
            ) : null}
          </article>

          {isAdmin ? (
            <div className="machineSubNav widePanel">
              <button
                className={activeMachineView === "overview" ? "subNavButton activeSubNavButton" : "subNavButton"}
                type="button"
                onClick={() => setMachineView("overview")}
              >
                {t("machines.overview")}
              </button>
              <button
                className={activeMachineView === "config" ? "subNavButton activeSubNavButton" : "subNavButton"}
                type="button"
                onClick={() => setMachineView("config")}
              >
                {t("machines.config")}
              </button>
            </div>
          ) : null}

          {activeMachineView === "overview" ? (
            <>
              <article className="panel powerCyclePanel">
                <div className="panelTitle">
                  <Power aria-hidden="true" size={22} />
                  <span>{t("machines.powerCycle")}</span>
                </div>
                <div className="powerCycleGrid">
                  {(["WOL", "REBOOT", "SHUTDOWN"] as MachineFunctionType[]).map((functionType: MachineFunctionType): ReactElement => {
                    const machineFunction: MachineFunctionResponse | undefined = functions.find(
                      (candidate: MachineFunctionResponse): boolean => candidate.type === functionType
                    );
                    const isWake: boolean = functionType === "WOL";
                    const canExecuteFunction: boolean =
                      canExecuteCommands &&
                      (isWake
                        ? machineFunction?.enabled === true && !machineOnline
                        : machineOnline && machineFunction?.enabled === true && machineFunction.scriptConfiguration !== null);

                    return (
                      <button
                        className={functionType === "SHUTDOWN" ? "powerButton dangerPowerButton" : "powerButton"}
                        disabled={!canExecuteFunction}
                        key={functionType}
                        type="button"
                        onClick={() => void onExecuteMachineFunction(functionType)}
                      >
                        <PowerIcon functionType={functionType} />
                        <span>{t(machineFunctionLabelKey(functionType))}</span>
                      </button>
                    );
                  })}
                </div>
              </article>

              <article className="panel actionPanel widePanel">
                <div className="panelTitle">
                  <Play aria-hidden="true" size={22} />
                  <span>{t("machines.actions")}</span>
                </div>
                {enabledConfigurations.length === 0 ? (
                  <p className="hintText">{t("machines.noScriptButtons")}</p>
                ) : (
                  <div className="scriptButtonMatrix">
                    {enabledConfigurations.map((configuration: ScriptButtonConfigurationResponse): ReactElement => (
                      <button
                        className="scriptMatrixButton"
                        disabled={!canExecuteCommands || !machineOnline}
                        key={configuration.id}
                        type="button"
                        onClick={() => void onExecuteScriptButtonConfiguration(configuration.id)}
                      >
                        <Play aria-hidden="true" size={18} />
                        <span>{configuration.label}</span>
                      </button>
                    ))}
                  </div>
                )}
              </article>

              <article className="panel statusPanel">
                <div className="panelTitle">
                  <RefreshCw aria-hidden="true" size={22} />
                  <span>{t("machines.statusChecks")}</span>
                </div>
                {displayedStatusChecks.length === 0 ? (
                  <p className="hintText">{t("machines.noStatusChecks")}</p>
                ) : (
                  <div className="statusCheckList">
                    {displayedStatusChecks.map((statusCheck: DisplayedStatusCheck): ReactElement => (
                      <div className="statusCheckItem" key={statusCheck.id} title={statusCheck.title}>
                        <span className={statusCheckDotClassName(statusCheck.healthy)} />
                        <strong>{statusCheck.label}</strong>
                        <small>{formatNullableDateTime(statusCheck.checkedAt, language)}</small>
                      </div>
                    ))}
                  </div>
                )}
              </article>

              <article className="panel statusPanel">
                <div className="panelTitle">
                  <RefreshCw aria-hidden="true" size={22} />
                  <span>{t("machines.stats")}</span>
                </div>
                {stats === null ? (
                  <p className="hintText">-</p>
                ) : (
                  <dl>
                    <div>
                      <dt>{t("machines.cpuLoad")}</dt>
                      <dd>{Math.round(stats.cpuLoad * 100)}%</dd>
                    </div>
                    <div>
                      <dt>{t("machines.memory")}</dt>
                      <dd>{stats.memoryUsedPercent.toFixed(1)}%</dd>
                    </div>
                    <div>
                      <dt>{t("machines.uptime")}</dt>
                      <dd>{formatUptime(stats.uptimeSeconds)}</dd>
                    </div>
                  </dl>
                )}
              </article>

              <MachineSection title={t("machines.network")} emptyText={t("machines.noNetwork")}>
                {networkInterfaces.map((networkInterface: MachineNetworkInterfaceResponse): ReactElement => (
                  <div className="compactListItem" key={networkInterface.id}>
                    <strong>{networkInterface.displayName ?? networkInterface.interfaceName}</strong>
                    <span>
                      {networkInterface.ipAddress ?? "-"} {networkInterface.macAddress ?? ""}
                    </span>
                    <small>
                      {networkInterface.family} - {networkInterface.up ? "UP" : "DOWN"}
                    </small>
                  </div>
                ))}
              </MachineSection>

              <MachineSection title={t("machines.commands")} emptyText={t("machines.noCommands")}>
                {commands.map((command: CommandResponse): ReactElement => (
                  <div className="compactListItem" key={command.id}>
                    <strong>{command.commandType}</strong>
                    <span>{command.status}</span>
                    <small>{formatNullableDateTime(command.createdAt, language)}</small>
                  </div>
                ))}
              </MachineSection>
            </>
          ) : null}

          {activeMachineView === "config" && isAdmin ? (
            <article className="panel statusPanel widePanel">
              <div className="panelTitle">
                <Pencil aria-hidden="true" size={22} />
                <span>{t("machines.rename")}</span>
              </div>
              <div className="commandForm">
                <label>
                  <span>{t("machines.displayName")}</span>
                  <input value={machineRenameValue} onChange={onMachineRenameValueChange} />
                </label>
                <div className="commandActions">
                  <button className="primaryButton iconTextButton" type="button" onClick={() => void onRenameMachine()}>
                    <Save aria-hidden="true" size={18} />
                    <span>{t("machines.saveName")}</span>
                  </button>
                </div>
              </div>
            </article>
          ) : null}

          {activeMachineView === "config" && isAdmin ? (
            <article className="panel adminPanel widePanel">
              <div className="panelTitle">
                <Pencil aria-hidden="true" size={22} />
                <span>{t("machines.statusCheckAdmin")}</span>
              </div>
              <div className="adminConfigGrid">
                <div className="commandForm">
                  <label>
                    <span>{t("machines.scripts")}</span>
                    <select
                      value={statusCheckConfigurationForm.scriptDefinitionId}
                      onChange={onStatusCheckConfigurationScriptChange}
                    >
                      <option value="">-</option>
                      {scripts.map((script: ScriptDefinitionResponse): ReactElement => (
                        <option key={script.id} value={script.id}>
                          {script.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    <span>{t("machines.statusLabel")}</span>
                    <input
                      value={statusCheckConfigurationForm.label}
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onStatusCheckConfigurationFieldChange("label", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    <span>{t("machines.intervalSeconds")}</span>
                    <input
                      inputMode="numeric"
                      value={statusCheckConfigurationForm.intervalSeconds}
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onStatusCheckConfigurationFieldChange("intervalSeconds", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    <span>{t("machines.sortOrder")}</span>
                    <input
                      inputMode="numeric"
                      value={statusCheckConfigurationForm.sortOrder}
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onStatusCheckConfigurationFieldChange("sortOrder", event.target.value)
                      }
                    />
                  </label>
                  <label className="checkboxField">
                    <input
                      checked={statusCheckConfigurationForm.enabled}
                      type="checkbox"
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onStatusCheckConfigurationFieldChange("enabled", event.target.checked)
                      }
                    />
                    <span>{t("machines.enabled")}</span>
                  </label>
                  <div className="scriptParameterGrid">
                    {statusCheckParameterDefinitions.length === 0 ? (
                      <p className="hintText">{t("machines.noParameters")}</p>
                    ) : (
                      statusCheckParameterDefinitions.map((parameter: ScriptParameterDefinition): ReactElement => (
                        <ScriptParameterField
                          key={parameter.name}
                          parameter={parameter}
                          t={t}
                          value={statusCheckConfigurationForm.parameterValues[parameter.name]}
                          onChange={onStatusCheckConfigurationParameterChange}
                        />
                      ))
                    )}
                  </div>
                  <div className="commandActions">
                    <button className="primaryButton iconTextButton" type="button" onClick={() => void onSaveStatusCheckConfiguration()}>
                      <Save aria-hidden="true" size={18} />
                      <span>{t("machines.saveStatusCheck")}</span>
                    </button>
                    <button className="secondaryButton iconTextButton" type="button" onClick={onCancelStatusCheckConfigurationEdit}>
                      <X aria-hidden="true" size={18} />
                      <span>{t("machines.cancelEdit")}</span>
                    </button>
                  </div>
                </div>

                <div className="commandForm">
                  <div className="compactList">
                    {statusCheckConfigurations.length === 0 ? (
                      <p className="hintText">{t("machines.noStatusCheckConfigurations")}</p>
                    ) : (
                      statusCheckConfigurations.map((configuration: StatusCheckConfigurationResponse): ReactElement => (
                        <div className="compactListItem configListItem" key={configuration.id}>
                          <strong>{configuration.label}</strong>
                          <span>
                            {configuration.enabled ? t("machines.enabled") : t("machines.disabled")} - {configuration.intervalSeconds}s
                          </span>
                          <div className="rowActions">
                            <button className="iconButton" type="button" onClick={() => onEditStatusCheckConfiguration(configuration)}>
                              <Pencil aria-hidden="true" size={17} />
                            </button>
                            <button
                              className="iconButton dangerButton"
                              type="button"
                              onClick={() => void onDeleteStatusCheckConfiguration(configuration)}
                            >
                              <Trash2 aria-hidden="true" size={17} />
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </article>
          ) : null}

          {activeMachineView === "config" && isAdmin ? (
            <article className="panel statusPanel widePanel">
              <div className="panelTitle">
                <Power aria-hidden="true" size={22} />
                <span>{t("machines.wakeOnLanConfiguration")}</span>
              </div>
              <div className="commandForm">
                <label>
                  <span>{t("machines.wakeOnLanInterface")}</span>
                  <select
                    value={selectedMachine.primaryWolInterfaceId ?? ""}
                    onChange={(event: ChangeEvent<HTMLSelectElement>) => void onWakeOnLanInterfaceChange(event.target.value)}
                  >
                    <option value="">{t("machines.wakeOnLanDisabled")}</option>
                    {wakeOnLanInterfaces.map((networkInterface: MachineNetworkInterfaceResponse): ReactElement => (
                      <option key={networkInterface.id} value={networkInterface.id}>
                        {formatNetworkInterfaceLabel(networkInterface)}
                      </option>
                    ))}
                  </select>
                </label>
                {wakeOnLanInterfaces.length === 0 ? (
                  <p className="hintText">{t("machines.noWakeOnLanInterfaces")}</p>
                ) : null}
              </div>
            </article>
          ) : null}

          {activeMachineView === "config" && isAdmin ? (
            <article className="panel adminPanel widePanel">
              <div className="panelTitle">
                <Pencil aria-hidden="true" size={22} />
                <span>{t("machines.scriptButtonAdmin")}</span>
              </div>
              <div className="commandActions">
                <button className="secondaryButton iconTextButton" type="button" onClick={() => void onRefreshStats()}>
                  <RefreshCw aria-hidden="true" size={18} />
                  <span>{t("machines.refreshStats")}</span>
                </button>
                <button className="secondaryButton iconTextButton" type="button" onClick={() => void onRefreshScriptManifest()}>
                  <RefreshCw aria-hidden="true" size={18} />
                  <span>{t("machines.refreshScripts")}</span>
                </button>
              </div>
              <div className="adminConfigGrid">
                <div className="commandForm">
                  <label>
                    <span>{t("machines.scripts")}</span>
                    <select
                      value={scriptButtonConfigurationForm.scriptDefinitionId}
                      onChange={onScriptButtonConfigurationScriptChange}
                    >
                      <option value="">-</option>
                      {scripts.map((script: ScriptDefinitionResponse): ReactElement => (
                        <option key={script.id} value={script.id}>
                          {script.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    <span>{t("machines.buttonLabel")}</span>
                    <input
                      value={scriptButtonConfigurationForm.label}
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onScriptButtonConfigurationFieldChange("label", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    <span>{t("machines.sortOrder")}</span>
                    <input
                      inputMode="numeric"
                      value={scriptButtonConfigurationForm.sortOrder}
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onScriptButtonConfigurationFieldChange("sortOrder", event.target.value)
                      }
                    />
                  </label>
                  <label className="checkboxField">
                    <input
                      checked={scriptButtonConfigurationForm.enabled}
                      type="checkbox"
                      onChange={(event: ChangeEvent<HTMLInputElement>) =>
                        onScriptButtonConfigurationFieldChange("enabled", event.target.checked)
                      }
                    />
                    <span>{t("machines.enabled")}</span>
                  </label>
                  <div className="scriptParameterGrid">
                    {parameterDefinitions.length === 0 ? (
                      <p className="hintText">{t("machines.noParameters")}</p>
                    ) : (
                      parameterDefinitions.map((parameter: ScriptParameterDefinition): ReactElement => (
                        <ScriptParameterField
                          key={parameter.name}
                          parameter={parameter}
                          t={t}
                          value={scriptButtonConfigurationForm.parameterValues[parameter.name]}
                          onChange={onScriptButtonConfigurationParameterChange}
                        />
                      ))
                    )}
                  </div>
                  <div className="commandActions">
                    <button className="primaryButton iconTextButton" type="button" onClick={() => void onSaveScriptButtonConfiguration()}>
                      <Save aria-hidden="true" size={18} />
                      <span>{t("machines.saveConfiguration")}</span>
                    </button>
                    <button className="secondaryButton iconTextButton" type="button" onClick={onCancelScriptButtonConfigurationEdit}>
                      <X aria-hidden="true" size={18} />
                      <span>{t("machines.cancelEdit")}</span>
                    </button>
                  </div>
                </div>

                <div className="commandForm">
                  <MachineFunctionAssignmentSelect
                    configurations={scriptButtonConfigurations}
                    functionType="REBOOT"
                    functions={functions}
                    t={t}
                    onAssign={onAssignMachineFunction}
                  />
                  <MachineFunctionAssignmentSelect
                    configurations={scriptButtonConfigurations}
                    functionType="SHUTDOWN"
                    functions={functions}
                    t={t}
                    onAssign={onAssignMachineFunction}
                  />
                  <div className="compactList">
                    {scriptButtonConfigurations.length === 0 ? (
                      <p className="hintText">{t("machines.noScriptButtons")}</p>
                    ) : (
                      scriptButtonConfigurations.map((configuration: ScriptButtonConfigurationResponse): ReactElement => (
                        <div className="compactListItem configListItem" key={configuration.id}>
                          <strong>{configuration.label}</strong>
                          <span>{configuration.enabled ? t("machines.enabled") : t("machines.disabled")}</span>
                          <div className="rowActions">
                            <button className="iconButton" type="button" onClick={() => onEditScriptButtonConfiguration(configuration)}>
                              <Pencil aria-hidden="true" size={17} />
                            </button>
                            <button
                              className="iconButton dangerButton"
                              type="button"
                              onClick={() => void onDeleteScriptButtonConfiguration(configuration)}
                            >
                              <Trash2 aria-hidden="true" size={17} />
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </article>
          ) : null}

          {activeMachineView === "config" && isAdmin ? (
            <article className="panel statusPanel widePanel">
              <div className="panelTitle">
                <span>{t("machines.agent")}</span>
              </div>
              <div className="compactList">
                {selectedMachine.agent === null ? (
                  <p className="hintText">-</p>
                ) : (
                  <div className="compactListItem">
                    <strong>{selectedMachine.agent.displayName}</strong>
                    <span>
                      {selectedMachine.agent.status} - {selectedMachine.agent.agentVersion}
                    </span>
                    <small>{selectedMachine.agent.capabilities.join(", ")}</small>
                  </div>
                )}
              </div>
            </article>
          ) : null}
        </section>
      )}
    </section>
  );
}

interface ScriptParameterDefinition {
  allowedValues: string[];
  defaultValue: string | boolean | null;
  name: string;
  required: boolean;
  type: string;
}

interface DisplayedStatusCheck {
  checkedAt: string | null;
  healthy: boolean | null;
  id: string;
  label: string;
  title: string;
}

interface RawParameterDefinition {
  allowedValues?: unknown;
  default?: unknown;
  required?: unknown;
  type?: unknown;
}

interface ScriptParameterFieldProps {
  parameter: ScriptParameterDefinition;
  t: (key: MessageKey) => string;
  value: string | boolean | undefined;
  onChange: (name: string, value: string | boolean) => void;
}

function ScriptParameterField({ parameter, t, value, onChange }: ScriptParameterFieldProps): ReactElement {
  const fieldLabel: string = parameter.required ? `${parameter.name} *` : parameter.name;

  if (parameter.type === "boolean" || parameter.type === "bool") {
    return (
      <label className="checkboxField">
        <input
          checked={Boolean(value)}
          type="checkbox"
          onChange={(event: ChangeEvent<HTMLInputElement>) => onChange(parameter.name, event.target.checked)}
        />
        <span>{fieldLabel}</span>
      </label>
    );
  }

  if (parameter.allowedValues.length > 0 || parameter.type === "enum") {
    return (
      <label>
        <span>{fieldLabel}</span>
        <select
          value={typeof value === "string" ? value : ""}
          onChange={(event: ChangeEvent<HTMLSelectElement>) => onChange(parameter.name, event.target.value)}
        >
          {!parameter.required ? <option value="">-</option> : null}
          {parameter.allowedValues.map((allowedValue: string): ReactElement => (
            <option key={allowedValue} value={allowedValue}>
              {allowedValue}
            </option>
          ))}
        </select>
      </label>
    );
  }

  return (
    <label>
      <span>{fieldLabel}</span>
      <input
        inputMode={parameter.type === "number" || parameter.type === "integer" || parameter.type === "int" ? "decimal" : "text"}
        placeholder={t("machines.parameterValue")}
        value={typeof value === "string" ? value : ""}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onChange(parameter.name, event.target.value)}
      />
    </label>
  );
}

function MachineFunctionAssignmentSelect({
  configurations,
  functionType,
  functions,
  t,
  onAssign
}: {
  configurations: ScriptButtonConfigurationResponse[];
  functionType: "REBOOT" | "SHUTDOWN";
  functions: MachineFunctionResponse[];
  t: (key: MessageKey) => string;
  onAssign: (functionType: MachineFunctionType, configurationId: string) => Promise<void>;
}): ReactElement {
  const assignedFunction: MachineFunctionResponse | undefined = functions.find(
    (machineFunction: MachineFunctionResponse): boolean => machineFunction.type === functionType
  );

  return (
    <label>
      <span>{t(machineFunctionLabelKey(functionType))}</span>
      <select
        value={assignedFunction?.scriptConfiguration?.id ?? ""}
        onChange={(event: ChangeEvent<HTMLSelectElement>) => void onAssign(functionType, event.target.value)}
      >
        <option value="">{t("machines.unassigned")}</option>
        {configurations.map((configuration: ScriptButtonConfigurationResponse): ReactElement => (
          <option key={configuration.id} value={configuration.id}>
            {configuration.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function PowerIcon({ functionType }: { functionType: MachineFunctionType }): ReactElement {
  if (functionType === "REBOOT") {
    return <RotateCw aria-hidden="true" size={22} />;
  }

  if (functionType === "SHUTDOWN") {
    return <Square aria-hidden="true" size={22} />;
  }

  return <Power aria-hidden="true" size={22} />;
}

function machineFunctionLabelKey(functionType: MachineFunctionType): MessageKey {
  if (functionType === "REBOOT") {
    return "machines.reboot";
  }

  if (functionType === "SHUTDOWN") {
    return "machines.shutdown";
  }

  return "machines.wakeOnLan";
}

function statusCheckDotClassName(healthy: boolean | null): string {
  if (healthy === true) {
    return "statusCheckDot healthyStatusCheckDot";
  }

  if (healthy === false) {
    return "statusCheckDot unhealthyStatusCheckDot";
  }

  return "statusCheckDot undefinedStatusCheckDot";
}

function machineListItemClassName(machine: MachineResponse, isSelected: boolean): string {
  const statusClass: string =
    machine.status === "ONLINE"
      ? "onlineMachineListItem"
      : machine.status === "OFFLINE"
        ? "offlineMachineListItem"
        : "unknownMachineListItem";
  return ["machineListItem", statusClass, isSelected ? "activeMachineListItem" : ""]
    .filter((className: string): boolean => className !== "")
    .join(" ");
}

function parseParameterSchema(parameterSchemaJson: string): ScriptParameterDefinition[] {
  try {
    const schema: unknown = JSON.parse(parameterSchemaJson);
    if (typeof schema !== "object" || schema === null || Array.isArray(schema)) {
      return [];
    }

    return Object.entries(schema).map(([name, rawDefinition]: [string, unknown]): ScriptParameterDefinition => {
      const definition: RawParameterDefinition =
        typeof rawDefinition === "object" && rawDefinition !== null && !Array.isArray(rawDefinition)
          ? (rawDefinition as RawParameterDefinition)
          : {};
      const allowedValues: string[] = Array.isArray(definition.allowedValues)
        ? definition.allowedValues.map((value: unknown): string => String(value))
        : [];
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
        required: definition.required === true,
        type: typeof definition.type === "string" ? definition.type : "string"
      };
    });
  } catch {
    return [];
  }
}

interface MachineSectionProps {
  children: ReactElement[];
  emptyText: string;
  title: string;
}

function MachineSection({ children, emptyText, title }: MachineSectionProps): ReactElement {
  return (
    <article className="panel statusPanel">
      <div className="panelTitle">
        <span>{title}</span>
      </div>
      {children.length === 0 ? <p className="hintText">{emptyText}</p> : <div className="compactList">{children}</div>}
    </article>
  );
}

function formatNullableDateTime(value: string | null, language: LanguageCode): string {
  if (value === null) {
    return "-";
  }

  return new Intl.DateTimeFormat(language, {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
}

function formatUptime(totalSeconds: number): string {
  const secondsPerMinute: number = 60;
  const secondsPerHour: number = secondsPerMinute * 60;
  const secondsPerDay: number = secondsPerHour * 24;
  const secondsPerYear: number = secondsPerDay * 365;
  const normalizedSeconds: number = Math.max(0, Math.floor(totalSeconds));
  const years: number = Math.floor(normalizedSeconds / secondsPerYear);
  const days: number = Math.floor((normalizedSeconds % secondsPerYear) / secondsPerDay);
  const hours: number = Math.floor((normalizedSeconds % secondsPerDay) / secondsPerHour);
  const minutes: number = Math.floor((normalizedSeconds % secondsPerHour) / secondsPerMinute);
  const seconds: number = normalizedSeconds % secondsPerMinute;
  const clock: string = [hours, minutes, seconds]
    .map((value: number): string => String(value).padStart(2, "0"))
    .join(":");

  return `${years}y ${days}d ${clock}`;
}

function formatNetworkInterfaceLabel(networkInterface: MachineNetworkInterfaceResponse): string {
  const displayName: string = networkInterface.displayName ?? networkInterface.interfaceName;
  const ipAddress: string = networkInterface.ipAddress === null ? "-" : networkInterface.ipAddress;
  const macAddress: string = networkInterface.macAddress === null ? "-" : networkInterface.macAddress;
  return `${displayName} - ${ipAddress} - ${macAddress}`;
}
