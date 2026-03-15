type Status =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "PAID"
  | "FAILED"
  | "PROCESSING";

interface StatusBadgeProps {
  status: string;
}

const statusMap: Record<Status, { cls: string; label: string }> = {
  PENDING: { cls: "tag tag-yellow", label: "Pendente" },
  CONFIRMED: { cls: "tag tag-green", label: "Confirmado" },
  CANCELLED: { cls: "tag tag-red", label: "Cancelado" },
  PAID: { cls: "tag tag-green", label: "Pago" },
  FAILED: { cls: "tag tag-red", label: "Falhou" },
  PROCESSING: { cls: "tag tag-purple", label: "Processando" },
};

export default function StatusBadge({ status }: StatusBadgeProps) {
  const mapped = statusMap[status as Status];

  if (!mapped) {
    return <span className="tag tag-purple">{status}</span>;
  }

  return <span className={mapped.cls}>{mapped.label}</span>;
}
