import { Monitor, Server } from "lucide-react";
import { type ReactElement } from "react";
import {
  type MachineResponse,
  type ServerStatusResponse
} from "../api/client";
import { type LanguageCode, type MessageKey } from "../i18n/messages";

export interface DashboardPageProps {
  language: LanguageCode;
  machines: MachineResponse[];
  status: ServerStatusResponse | null;
  t: (key: MessageKey) => string;
  onSelectMachine: (machineId: string) => Promise<void>;
}

export function DashboardPage({
  language,
  machines,
  status,
  t,
  onSelectMachine
}: DashboardPageProps): ReactElement {
  const formattedTimestamp: string = formatDateTime(status?.timestamp ?? null, language);
  const onlineMachineCount: number = machines.filter((machine: MachineResponse): boolean => machine.status === "ONLINE").length;

  return (
    <div className="statusGrid">
      <article className="panel statusPanel widePanel">
        <div className="panelTitle">
          <Server aria-hidden="true" size={22} />
          <span>{t("dashboard.server")}</span>
        </div>
        {status === null ? (
          <p>{t("dashboard.unavailable")}</p>
        ) : (
          <>
            <strong>{status.application}</strong>
            <dl>
              <div>
                <dt>{t("dashboard.status")}</dt>
                <dd>{status.status}</dd>
              </div>
              <div>
                <dt>{t("dashboard.lastUpdated")}</dt>
                <dd>{formattedTimestamp}</dd>
              </div>
            </dl>
          </>
        )}
      </article>
      <MetricCard label={t("dashboard.machines")} value={status?.machineCount ?? 0} />
      <MetricCard label={t("dashboard.onlineMachines")} value={onlineMachineCount} />
      <MetricCard label={t("dashboard.commands")} value={status?.commandCount ?? 0} />
      <article className="panel statusPanel widePanel">
        <div className="panelTitle">
          <Monitor aria-hidden="true" size={22} />
          <span>{t("dashboard.machines")}</span>
        </div>
        {machines.length === 0 ? (
          <p className="hintText">{t("machines.noMachines")}</p>
        ) : (
          <div className="dashboardMachineGrid">
            {machines.map((machine: MachineResponse): ReactElement => (
              <button
                className={dashboardMachineButtonClassName(machine)}
                key={machine.id}
                type="button"
                onClick={() => void onSelectMachine(machine.id)}
              >
                <strong>{machine.displayName}</strong>
                <span>{machine.hostname}</span>
                <small className="machineStatusLabel">{machine.status}</small>
              </button>
            ))}
          </div>
        )}
      </article>
    </div>
  );
}

function formatDateTime(value: string | null, language: LanguageCode): string {
  if (value === null) {
    return "-";
  }

  return new Intl.DateTimeFormat(language, {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
}

interface MetricCardProps {
  label: string;
  value: number;
}

function MetricCard({ label, value }: MetricCardProps): ReactElement {
  return (
    <article className="panel metricPanel">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function dashboardMachineButtonClassName(machine: MachineResponse): string {
  const statusClass: string =
    machine.status === "ONLINE"
      ? "onlineMachineListItem"
      : machine.status === "OFFLINE"
        ? "offlineMachineListItem"
        : "unknownMachineListItem";
  return ["dashboardMachineButton", statusClass].join(" ");
}
