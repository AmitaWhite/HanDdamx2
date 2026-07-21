import { useEffect } from "react";
import { Button } from "@/components/ui/Button";

interface ConfirmDialogProps {
	open: boolean;
	title: string;
	description?: string;
	confirmLabel?: string;
	cancelLabel?: string;
	onConfirm: () => void;
	onCancel: () => void;
}

export function ConfirmDialog({
	open,
	title,
	description,
	confirmLabel = "확인",
	cancelLabel = "취소",
	onConfirm,
	onCancel,
}: ConfirmDialogProps) {
	useEffect(() => {
		if (!open) return;

		function onKeyDown(e: KeyboardEvent) {
			if (e.key === "Escape") onCancel();
		}

		window.addEventListener("keydown", onKeyDown);
		return () => window.removeEventListener("keydown", onKeyDown);
	}, [open, onCancel]);

	if (!open) return null;

	return (
		<div
			className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
			onClick={onCancel}
		>
			<div
				role="dialog"
				aria-modal="true"
				aria-labelledby="confirm-dialog-title"
				className="w-full max-w-sm rounded-xl bg-surface-container-lowest p-6 shadow-lg"
				onClick={(e) => e.stopPropagation()}
			>
				<h2 id="confirm-dialog-title" className="text-title-md font-title-md text-on-surface">
					{title}
				</h2>
				{description && (
					<p className="mt-2 text-body-md text-secondary">{description}</p>
				)}
				<div className="mt-6 flex justify-end gap-3">
					<Button type="button" variant="ghost" autoFocus onClick={onCancel}>
						{cancelLabel}
					</Button>
					<Button type="button" variant="primary" onClick={onConfirm}>
						{confirmLabel}
					</Button>
				</div>
			</div>
		</div>
	);
}