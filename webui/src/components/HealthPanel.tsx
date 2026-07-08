import { Activity } from "lucide-react";
import { type ReactElement } from "react";
import { type ActuatorHealthResponse } from "../api/client";
import { type MessageKey } from "../i18n/messages";

export interface HealthPanelProps {
  health: ActuatorHealthResponse | null;
  t: (key: MessageKey) => string;
}

export function HealthPanel({ health, t }: HealthPanelProps): ReactElement {
  return (
    <article className="panel statusPanel">
      <div className="panelTitle">
        <Activity aria-hidden="true" size={22} />
        <span>{t("dashboard.health")}</span>
      </div>
      <dl>
        <div>
          <dt>{t("common.endpoint")}</dt>
          <dd>/actuator/health</dd>
        </div>
        <div>
          <dt>{t("dashboard.status")}</dt>
          <dd>{health?.status ?? "-"}</dd>
        </div>
      </dl>
    </article>
  );
}
