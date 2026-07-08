import React from "react";
import { createRoot, type Root } from "react-dom/client";
import { App } from "./App";
import "./styles/app.css";

const rootElement: HTMLElement | null = document.getElementById("root");

if (rootElement === null) {
  throw new Error("Root element not found");
}

const root: Root = createRoot(rootElement);
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
