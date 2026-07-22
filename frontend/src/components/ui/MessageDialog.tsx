import { useEffect } from "react";
import { Button } from "@/components/ui/Button";

interface MessageDialogProps {
	open: boolean;
	title?: string;
	message: string;
	confirmLabel?: string;
	onClose: () => void;
}

/** 확인 버튼 하나만 있는 안내/에러 메시지 모달. 취소가 필요한 경우엔 ConfirmDialog를 쓴다. */
export function MessageDialog({
	open,
	title = "안내",
	message,
	confirmLabel = "확인",
	onClose,
}: MessageDialogProps) {
	useEffect(() => {
		if (!open) return;

		function onKeyDown(e: KeyboardEvent) {
			if (e.key === "Escape") onClose();
		}

		window.addEventListener("keydown", onKeyDown);
		return () => window.removeEventListener("keydown", onKeyDown);
	}, [open, onClose]);

	if (!open) return null;

	return (
		<div
			className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
			onClick={onClose}
		>
			<div
				role="alertdialog"
				aria-modal="true"
				aria-labelledby="message-dialog-title"
				className="w-full max-w-sm rounded-xl bg-surface-container-lowest p-6 shadow-lg"
				onClick={(e) => e.stopPropagation()}
			>
				<h2 id="message-dialog-title" className="text-title-md font-title-md text-on-surface">
					{title}
				</h2>
				<p className="mt-2 text-body-md text-secondary">{message}</p>
				<div className="mt-6 flex justify-end">
					<Button type="button" variant="primary" autoFocus onClick={onClose}>
						{confirmLabel}
					</Button>
				</div>
			</div>
		</div>
	);
}
