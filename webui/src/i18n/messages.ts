export type LanguageCode = "en" | "de";

export type MessageKey =
  | "app.title"
  | "app.subtitle"
  | "auth.username"
  | "auth.password"
  | "auth.currentPassword"
  | "auth.newPassword"
  | "auth.confirmPassword"
  | "auth.signIn"
  | "auth.signOut"
  | "auth.changePassword"
  | "auth.cancelPasswordChange"
  | "auth.sessionExpired"
  | "auth.passwordChangeRequired"
  | "auth.passwordChanged"
  | "auth.passwordPolicy"
  | "auth.passwordMismatch"
  | "auth.passwordSameAsCurrent"
  | "auth.passwordSameAsUsername"
  | "common.loading"
  | "common.retry"
  | "common.language"
  | "common.endpoint"
  | "common.yes"
  | "common.no"
  | "common.copy"
  | "common.copied"
  | "nav.dashboard"
  | "nav.enrollment"
  | "nav.machines"
  | "nav.users"
  | "account.menu"
  | "dashboard.server"
  | "dashboard.status"
  | "dashboard.health"
  | "dashboard.user"
  | "dashboard.role"
  | "dashboard.tokenExpires"
  | "dashboard.machines"
  | "dashboard.onlineMachines"
  | "dashboard.commands"
  | "dashboard.lastUpdated"
  | "dashboard.signedInAs"
  | "dashboard.unavailable"
  | "dashboard.implementedApi"
  | "dashboard.authApi"
  | "dashboard.meApi"
  | "dashboard.statusApi"
  | "dashboard.healthApi"
  | "dashboard.usersApi"
  | "dashboard.enrollmentApi"
  | "dashboard.machinesApi"
  | "admin.users"
  | "admin.createUser"
  | "admin.username"
  | "admin.role"
  | "admin.created"
  | "admin.updated"
  | "admin.passwordRequired"
  | "admin.temporaryPassword"
  | "admin.temporaryPasswordHint"
  | "admin.resetPassword"
  | "admin.deleteUser"
  | "admin.noUsers"
  | "admin.userCreated"
  | "admin.roleUpdated"
  | "admin.passwordReset"
  | "admin.userDeleted"
  | "admin.confirmDelete"
  | "enrollment.tokens"
  | "enrollment.createToken"
  | "enrollment.suggestedName"
  | "enrollment.expiresIn"
  | "enrollment.expiresAt"
  | "enrollment.createdAt"
  | "enrollment.usedAt"
  | "enrollment.usedByAgent"
  | "enrollment.bundle"
  | "enrollment.bundleHint"
  | "enrollment.noTokens"
  | "enrollment.tokenCreated"
  | "enrollment.unused"
  | "enrollment.oneHour"
  | "enrollment.oneDay"
  | "enrollment.oneWeek"
  | "machines.title"
  | "machines.noMachines"
  | "machines.hostname"
  | "machines.status"
  | "machines.statusChecks"
  | "machines.noStatusChecks"
  | "machines.statusCheck"
  | "machines.statusCheckAdmin"
  | "machines.statusCheckDeleted"
  | "machines.statusCheckSaved"
  | "machines.statusLabel"
  | "machines.intervalSeconds"
  | "machines.saveStatusCheck"
  | "machines.noStatusCheckConfigurations"
  | "machines.confirmDeleteStatusCheck"
  | "machines.lastSeen"
  | "machines.agent"
  | "machines.capabilities"
  | "machines.stats"
  | "machines.cpuLoad"
  | "machines.memory"
  | "machines.uptime"
  | "machines.scripts"
  | "machines.noScripts"
  | "machines.network"
  | "machines.noNetwork"
  | "machines.commands"
  | "machines.displayName"
  | "machines.noCommands"
  | "machines.refreshStats"
  | "machines.refreshScripts"
  | "machines.executeScript"
  | "machines.actions"
  | "machines.buttonLabel"
  | "machines.cancelEdit"
  | "machines.configurationDeleted"
  | "machines.configurationSaved"
  | "machines.confirmDeleteConfiguration"
  | "machines.confirmDeleteMachine"
  | "machines.config"
  | "machines.deleteMachine"
  | "machines.disabled"
  | "machines.enabled"
  | "machines.functionAssigned"
  | "machines.noScriptButtons"
  | "machines.noWakeOnLanInterfaces"
  | "machines.overview"
  | "machines.powerCycle"
  | "machines.reboot"
  | "machines.rename"
  | "machines.renamed"
  | "machines.saveName"
  | "machines.saveConfiguration"
  | "machines.scriptButton"
  | "machines.scriptButtonAdmin"
  | "machines.shutdown"
  | "machines.sortOrder"
  | "machines.unassigned"
  | "machines.wakeOnLan"
  | "machines.wakeOnLanConfiguration"
  | "machines.wakeOnLanConfigured"
  | "machines.wakeOnLanDisabled"
  | "machines.wakeOnLanInterface"
  | "machines.noParameters"
  | "machines.parameterValue"
  | "machines.parametersJson"
  | "machines.macAddress"
  | "machines.machineDeleted"
  | "machines.broadcastAddress"
  | "machines.port"
  | "machines.commandSent"
  | "machines.invalidJson"
  | "role.ADMIN"
  | "role.OPERATOR"
  | "role.VIEWER";

type Messages = Record<MessageKey, string>;

const englishMessages: Messages = {
  "app.title": "Centaurus",
  "app.subtitle": "Remote administration for trusted machines",
  "auth.username": "Username",
  "auth.password": "Password",
  "auth.currentPassword": "Current password",
  "auth.newPassword": "New password",
  "auth.confirmPassword": "Confirm password",
  "auth.signIn": "Sign in",
  "auth.signOut": "Sign out",
  "auth.changePassword": "Change password",
  "auth.cancelPasswordChange": "Cancel",
  "auth.sessionExpired": "Your session expired. Sign in again.",
  "auth.passwordChangeRequired": "Password change is required before normal actions are available.",
  "auth.passwordChanged": "Password changed. Sign in with the new password.",
  "auth.passwordPolicy": "Use at least 12 characters.",
  "auth.passwordMismatch": "New password and confirmation must match.",
  "auth.passwordSameAsCurrent": "New password must differ from the current password.",
  "auth.passwordSameAsUsername": "New password must not match the username.",
  "common.loading": "Loading",
  "common.retry": "Retry",
  "common.language": "Language",
  "common.endpoint": "Endpoint",
  "common.yes": "Yes",
  "common.no": "No",
  "common.copy": "Copy",
  "common.copied": "Copied.",
  "nav.dashboard": "Dashboard",
  "nav.enrollment": "Enrollment",
  "nav.machines": "Machines",
  "nav.users": "Users",
  "account.menu": "Account menu",
  "dashboard.server": "Server",
  "dashboard.status": "Status",
  "dashboard.health": "Health",
  "dashboard.user": "User",
  "dashboard.role": "Role",
  "dashboard.tokenExpires": "Token expires",
  "dashboard.machines": "Machines",
  "dashboard.onlineMachines": "Online",
  "dashboard.commands": "Commands",
  "dashboard.lastUpdated": "Last updated",
  "dashboard.signedInAs": "Signed in as",
  "dashboard.unavailable": "Status is currently unavailable.",
  "dashboard.implementedApi": "Implemented API",
  "dashboard.authApi": "Login, refresh and logout",
  "dashboard.meApi": "Current user",
  "dashboard.statusApi": "Authenticated server status",
  "dashboard.healthApi": "Public actuator health",
  "dashboard.usersApi": "Admin user and role management",
  "dashboard.enrollmentApi": "Admin enrollment token management",
  "dashboard.machinesApi": "Machine registry and commands",
  "admin.users": "Users",
  "admin.createUser": "Create user",
  "admin.username": "Username",
  "admin.role": "Role",
  "admin.created": "Created",
  "admin.updated": "Updated",
  "admin.passwordRequired": "Password change required",
  "admin.temporaryPassword": "Temporary password",
  "admin.temporaryPasswordHint": "This password is shown only once.",
  "admin.resetPassword": "Reset password",
  "admin.deleteUser": "Delete user",
  "admin.noUsers": "No users found.",
  "admin.userCreated": "User created.",
  "admin.roleUpdated": "Role updated.",
  "admin.passwordReset": "Password reset.",
  "admin.userDeleted": "User deleted.",
  "admin.confirmDelete": "Delete this user?",
  "enrollment.tokens": "Enrollment tokens",
  "enrollment.createToken": "Create token",
  "enrollment.suggestedName": "Suggested name",
  "enrollment.expiresIn": "Expires in",
  "enrollment.expiresAt": "Expires at",
  "enrollment.createdAt": "Created",
  "enrollment.usedAt": "Used",
  "enrollment.usedByAgent": "Used by agent",
  "enrollment.bundle": "Enrollment bundle",
  "enrollment.bundleHint": "This bundle is shown only once. Use it in the local agent UI.",
  "enrollment.noTokens": "No enrollment tokens found.",
  "enrollment.tokenCreated": "Enrollment token created.",
  "enrollment.unused": "Unused",
  "enrollment.oneHour": "1 hour",
  "enrollment.oneDay": "1 day",
  "enrollment.oneWeek": "1 week",
  "machines.title": "Machines",
  "machines.noMachines": "No machines found.",
  "machines.hostname": "Hostname",
  "machines.status": "Status",
  "machines.statusChecks": "Status checks",
  "machines.noStatusChecks": "No status checks reported.",
  "machines.statusCheck": "Status check",
  "machines.statusCheckAdmin": "Status check configuration",
  "machines.statusCheckDeleted": "Status check deleted.",
  "machines.statusCheckSaved": "Status check saved.",
  "machines.statusLabel": "Status label",
  "machines.intervalSeconds": "Interval seconds",
  "machines.saveStatusCheck": "Save status check",
  "machines.noStatusCheckConfigurations": "No status checks configured.",
  "machines.confirmDeleteStatusCheck": "Delete this status check?",
  "machines.lastSeen": "Last seen",
  "machines.agent": "Agent",
  "machines.capabilities": "Capabilities",
  "machines.stats": "Stats",
  "machines.cpuLoad": "CPU load",
  "machines.memory": "Memory",
  "machines.uptime": "Uptime",
  "machines.scripts": "Scripts",
  "machines.noScripts": "No scripts reported.",
  "machines.network": "Network interfaces",
  "machines.noNetwork": "No network interfaces reported.",
  "machines.commands": "Commands",
  "machines.displayName": "Display name",
  "machines.noCommands": "No commands found.",
  "machines.refreshStats": "Refresh stats",
  "machines.refreshScripts": "Refresh scripts",
  "machines.executeScript": "Execute script",
  "machines.actions": "Actions",
  "machines.buttonLabel": "Button label",
  "machines.cancelEdit": "Cancel",
  "machines.configurationDeleted": "Script button deleted.",
  "machines.configurationSaved": "Script button saved.",
  "machines.confirmDeleteConfiguration": "Delete this script button?",
  "machines.confirmDeleteMachine": "Remove this machine and agent from the server? The agent needs a new enrollment token before it can register again.",
  "machines.config": "Config",
  "machines.deleteMachine": "Remove agent",
  "machines.disabled": "Disabled",
  "machines.enabled": "Enabled",
  "machines.functionAssigned": "Function assignment saved.",
  "machines.noScriptButtons": "No script buttons configured.",
  "machines.noWakeOnLanInterfaces": "No network interface with MAC address found.",
  "machines.overview": "Overview",
  "machines.powerCycle": "Power Cycle",
  "machines.reboot": "Reboot",
  "machines.rename": "Rename machine",
  "machines.renamed": "Machine renamed.",
  "machines.saveName": "Save name",
  "machines.saveConfiguration": "Save button",
  "machines.scriptButton": "Script button",
  "machines.scriptButtonAdmin": "Script button configuration",
  "machines.shutdown": "Shutdown",
  "machines.sortOrder": "Sort order",
  "machines.unassigned": "Unassigned",
  "machines.wakeOnLan": "Wake-on-LAN",
  "machines.wakeOnLanConfiguration": "Wake-on-LAN configuration",
  "machines.wakeOnLanConfigured": "Wake-on-LAN configuration saved.",
  "machines.wakeOnLanDisabled": "Disabled",
  "machines.wakeOnLanInterface": "Wake-on-LAN interface",
  "machines.noParameters": "No parameters required.",
  "machines.parameterValue": "Value",
  "machines.parametersJson": "Parameters JSON",
  "machines.macAddress": "MAC address",
  "machines.machineDeleted": "Agent removed from server.",
  "machines.broadcastAddress": "Broadcast address",
  "machines.port": "Port",
  "machines.commandSent": "Command sent.",
  "machines.invalidJson": "Parameters must be valid JSON.",
  "role.ADMIN": "Admin",
  "role.OPERATOR": "Operator",
  "role.VIEWER": "Viewer"
};

const germanMessages: Partial<Messages> = {
  "app.subtitle": "Remote-Verwaltung fuer vertrauenswuerdige Maschinen",
  "auth.username": "Benutzername",
  "auth.password": "Passwort",
  "auth.currentPassword": "Aktuelles Passwort",
  "auth.newPassword": "Neues Passwort",
  "auth.confirmPassword": "Passwort bestaetigen",
  "auth.signIn": "Anmelden",
  "auth.signOut": "Abmelden",
  "auth.changePassword": "Passwort aendern",
  "auth.cancelPasswordChange": "Abbrechen",
  "auth.sessionExpired": "Die Sitzung ist abgelaufen. Bitte erneut anmelden.",
  "auth.passwordChangeRequired": "Vor normalen Aktionen ist eine Passwortaenderung erforderlich.",
  "auth.passwordChanged": "Passwort geaendert. Bitte mit dem neuen Passwort anmelden.",
  "auth.passwordPolicy": "Mindestens 12 Zeichen verwenden.",
  "auth.passwordMismatch": "Neues Passwort und Bestaetigung muessen uebereinstimmen.",
  "auth.passwordSameAsCurrent": "Das neue Passwort muss sich vom aktuellen Passwort unterscheiden.",
  "auth.passwordSameAsUsername": "Das neue Passwort darf nicht dem Benutzernamen entsprechen.",
  "common.loading": "Laedt",
  "common.retry": "Erneut versuchen",
  "common.language": "Sprache",
  "common.endpoint": "Endpunkt",
  "common.yes": "Ja",
  "common.no": "Nein",
  "common.copy": "Kopieren",
  "common.copied": "Kopiert.",
  "nav.dashboard": "Dashboard",
  "nav.enrollment": "Enrollment",
  "nav.machines": "Maschinen",
  "nav.users": "Users",
  "account.menu": "Account Menu",
  "dashboard.server": "Server",
  "dashboard.status": "Status",
  "dashboard.health": "Health",
  "dashboard.user": "User",
  "dashboard.role": "Rolle",
  "dashboard.tokenExpires": "Token laeuft ab",
  "dashboard.machines": "Maschinen",
  "dashboard.onlineMachines": "Online",
  "dashboard.commands": "Commands",
  "dashboard.lastUpdated": "Zuletzt aktualisiert",
  "dashboard.signedInAs": "Angemeldet als",
  "dashboard.unavailable": "Status ist derzeit nicht verfuegbar.",
  "dashboard.implementedApi": "Implementierte API",
  "dashboard.authApi": "Login, Refresh und Logout",
  "dashboard.meApi": "Aktueller User",
  "dashboard.statusApi": "Authentifizierter Server-Status",
  "dashboard.healthApi": "Public Actuator Health",
  "dashboard.usersApi": "Admin User- und Rollenverwaltung",
  "dashboard.enrollmentApi": "Admin Enrollment-Token-Verwaltung",
  "dashboard.machinesApi": "Maschinen-Registry und Commands",
  "admin.users": "Users",
  "admin.createUser": "User anlegen",
  "admin.username": "Benutzername",
  "admin.role": "Rolle",
  "admin.created": "Erstellt",
  "admin.updated": "Aktualisiert",
  "admin.passwordRequired": "Passwortaenderung erforderlich",
  "admin.temporaryPassword": "Temporaeres Passwort",
  "admin.temporaryPasswordHint": "Dieses Passwort wird nur einmal angezeigt.",
  "admin.resetPassword": "Passwort zuruecksetzen",
  "admin.deleteUser": "User loeschen",
  "admin.noUsers": "Keine User gefunden.",
  "admin.userCreated": "User angelegt.",
  "admin.roleUpdated": "Rolle aktualisiert.",
  "admin.passwordReset": "Passwort zurueckgesetzt.",
  "admin.userDeleted": "User geloescht.",
  "admin.confirmDelete": "Diesen User loeschen?",
  "enrollment.tokens": "Enrollment Tokens",
  "enrollment.createToken": "Token erstellen",
  "enrollment.suggestedName": "Vorgeschlagener Name",
  "enrollment.expiresIn": "Gueltig fuer",
  "enrollment.expiresAt": "Gueltig bis",
  "enrollment.createdAt": "Erstellt",
  "enrollment.usedAt": "Verwendet",
  "enrollment.usedByAgent": "Verwendet von Agent",
  "enrollment.bundle": "Enrollment Bundle",
  "enrollment.bundleHint": "Dieses Bundle wird nur einmal angezeigt. Nutze es in der lokalen Agent-UI.",
  "enrollment.noTokens": "Keine Enrollment Tokens gefunden.",
  "enrollment.tokenCreated": "Enrollment Token erstellt.",
  "enrollment.unused": "Nicht verwendet",
  "enrollment.oneHour": "1 Stunde",
  "enrollment.oneDay": "1 Tag",
  "enrollment.oneWeek": "1 Woche",
  "machines.title": "Maschinen",
  "machines.noMachines": "Keine Maschinen gefunden.",
  "machines.hostname": "Hostname",
  "machines.status": "Status",
  "machines.statusChecks": "Status Checks",
  "machines.noStatusChecks": "Keine Status Checks gemeldet.",
  "machines.statusCheck": "Status Check",
  "machines.statusCheckAdmin": "Status-Check-Konfiguration",
  "machines.statusCheckDeleted": "Status Check geloescht.",
  "machines.statusCheckSaved": "Status Check gespeichert.",
  "machines.statusLabel": "Status-Label",
  "machines.intervalSeconds": "Intervall in Sekunden",
  "machines.saveStatusCheck": "Status Check speichern",
  "machines.noStatusCheckConfigurations": "Keine Status Checks konfiguriert.",
  "machines.confirmDeleteStatusCheck": "Diesen Status Check loeschen?",
  "machines.lastSeen": "Zuletzt gesehen",
  "machines.agent": "Agent",
  "machines.capabilities": "Capabilities",
  "machines.stats": "Stats",
  "machines.cpuLoad": "CPU Load",
  "machines.memory": "Speicher",
  "machines.uptime": "Uptime",
  "machines.scripts": "Scripts",
  "machines.noScripts": "Keine Scripts gemeldet.",
  "machines.network": "Network Interfaces",
  "machines.noNetwork": "Keine Network Interfaces gemeldet.",
  "machines.commands": "Commands",
  "machines.displayName": "Anzeigename",
  "machines.noCommands": "Keine Commands gefunden.",
  "machines.refreshStats": "Stats aktualisieren",
  "machines.refreshScripts": "Scripts aktualisieren",
  "machines.executeScript": "Script ausfuehren",
  "machines.actions": "Aktionen",
  "machines.buttonLabel": "Button-Label",
  "machines.cancelEdit": "Abbrechen",
  "machines.configurationDeleted": "Script-Button geloescht.",
  "machines.configurationSaved": "Script-Button gespeichert.",
  "machines.confirmDeleteConfiguration": "Diesen Script-Button loeschen?",
  "machines.confirmDeleteMachine": "Diese Maschine und ihren Agent vom Server entfernen? Der Agent braucht danach ein neues Enrollment Token, bevor er sich wieder registrieren kann.",
  "machines.config": "Config",
  "machines.deleteMachine": "Agent entfernen",
  "machines.disabled": "Deaktiviert",
  "machines.enabled": "Aktiviert",
  "machines.functionAssigned": "Function-Zuweisung gespeichert.",
  "machines.noScriptButtons": "Keine Script-Buttons konfiguriert.",
  "machines.noWakeOnLanInterfaces": "Kein Network Interface mit MAC-Adresse gefunden.",
  "machines.overview": "Uebersicht",
  "machines.powerCycle": "Power Cycle",
  "machines.reboot": "Reboot",
  "machines.rename": "Maschine umbenennen",
  "machines.renamed": "Maschine umbenannt.",
  "machines.saveName": "Name speichern",
  "machines.saveConfiguration": "Button speichern",
  "machines.scriptButton": "Script-Button",
  "machines.scriptButtonAdmin": "Script-Button-Konfiguration",
  "machines.shutdown": "Shutdown",
  "machines.sortOrder": "Sortierung",
  "machines.unassigned": "Nicht zugewiesen",
  "machines.wakeOnLan": "Wake-on-LAN",
  "machines.wakeOnLanConfiguration": "Wake-on-LAN-Konfiguration",
  "machines.wakeOnLanConfigured": "Wake-on-LAN-Konfiguration gespeichert.",
  "machines.wakeOnLanDisabled": "Deaktiviert",
  "machines.wakeOnLanInterface": "Wake-on-LAN-Interface",
  "machines.noParameters": "Keine Parameter erforderlich.",
  "machines.parameterValue": "Wert",
  "machines.parametersJson": "Parameter JSON",
  "machines.macAddress": "MAC-Adresse",
  "machines.machineDeleted": "Agent vom Server entfernt.",
  "machines.broadcastAddress": "Broadcast-Adresse",
  "machines.port": "Port",
  "machines.commandSent": "Command gesendet.",
  "machines.invalidJson": "Parameter muessen gueltiges JSON sein.",
  "role.ADMIN": "Admin",
  "role.OPERATOR": "Operator",
  "role.VIEWER": "Viewer"
};

const messagesByLanguage: Record<LanguageCode, Partial<Messages>> = {
  en: englishMessages,
  de: germanMessages
};

export function translate(language: LanguageCode, key: MessageKey): string {
  const localizedMessages: Partial<Messages> = messagesByLanguage[language];
  return localizedMessages[key] ?? englishMessages[key];
}
