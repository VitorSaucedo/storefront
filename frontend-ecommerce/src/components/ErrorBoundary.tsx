import { Component } from "react";
// Importação de tipos explícita para satisfazer 'verbatimModuleSyntax'
import type { ErrorInfo, ReactNode } from "react";

interface Props {
  children?: ReactNode;
}

interface State {
  hasError: boolean;
}

class ErrorBoundary extends Component<Props, State> {
  public state: State = { hasError: false };

  public static getDerivedStateFromError(_: Error): State {
    return { hasError: true };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error:", error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            padding: "2rem",
            textAlign: "center",
            color: "white",
            background: "var(--bg)",
          }}
        >
          <h1>Ops! Algo deu errado.</h1>
          <button
            onClick={() => window.location.reload()}
            style={{ padding: "10px 20px", cursor: "pointer" }}
          >
            Recarregar Página
          </button>
        </div>
      );
    }

    // Corrigido: Em componentes de classe, acessamos via this.props
    return this.props.children;
  }
}

export default ErrorBoundary;
