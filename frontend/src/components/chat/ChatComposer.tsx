import { useEffect, useRef, useState } from "react";
import { Icon } from "@/components/ui/Icon";

interface ChatComposerProps {
	canSendText: boolean;
	canSendImage: boolean;
	imageBusy: boolean;
	roomClosed: boolean;
	socketReady: boolean;
	onSendText: (text: string) => void;
	onSelectImage: (file: File) => void;
}

const IMAGE_ACCEPT = "image/jpeg,image/png,image/gif,image/webp";

/**
 * 채팅 입력창. 드래프트 상태는 컴포저 내부에서 관리한다.
 *
 * <ul>
 *   <li>Enter 로 텍스트 전송, Shift+Enter 로 줄바꿈</li>
 *   <li>한글 등 IME 조합 중(Enter) 전송은 무시 — 조합 확정만 처리</li>
 *   <li>+ 버튼으로 이미지 선택 → 부모의 REST 업로드에 위임</li>
 *   <li>placeholder 는 방 상태(종료/연결 중/전송 가능)에 따라 다르게 표시</li>
 * </ul>
 */
export function ChatComposer({
	canSendText,
	canSendImage,
	imageBusy,
	roomClosed,
	socketReady,
	onSendText,
	onSelectImage,
}: ChatComposerProps) {
	const [draft, setDraft] = useState("");
	const fileInputRef = useRef<HTMLInputElement>(null);
	const textareaRef = useRef<HTMLTextAreaElement>(null);

	// 여러 줄 입력에 맞춰 textarea 높이를 내용에 맞춰 늘린다. (max 5줄)
	useEffect(() => {
		const el = textareaRef.current;
		if (!el) return;
		el.style.height = "auto";
		el.style.height = `${Math.min(el.scrollHeight, 140)}px`;
	}, [draft]);

	function handleSend() {
		const text = draft.trim();
		if (!canSendText || !text) return;
		onSendText(text);
		setDraft("");
	}

	function handleFileChange(file: File | undefined) {
		if (!file || !canSendImage) return;
		onSelectImage(file);
		if (fileInputRef.current) fileInputRef.current.value = "";
	}

	const placeholder = roomClosed
		? "종료된 채팅방입니다"
		: !socketReady
			? "연결 중…"
			: "메시지를 입력하세요 (Shift+Enter 줄바꿈)";

	return (
		<div className="container-page flex items-end gap-2 border-t border-outline-variant/50 py-4">
			<input
				ref={fileInputRef}
				type="file"
				accept={IMAGE_ACCEPT}
				className="hidden"
				onChange={(e) => handleFileChange(e.target.files?.[0])}
			/>
			<button
				type="button"
				aria-label={imageBusy ? "이미지 업로드 중" : "이미지 첨부"}
				className="mb-1.5 text-secondary disabled:opacity-40"
				disabled={!canSendImage}
				onClick={() => fileInputRef.current?.click()}
			>
				<Icon name={imageBusy ? "hourglass_empty" : "add_circle"} />
			</button>
			<textarea
				ref={textareaRef}
				value={draft}
				onChange={(e) => setDraft(e.target.value)}
				onKeyDown={(e) => {
					if (
						e.key === "Enter" &&
						!e.shiftKey &&
						!e.nativeEvent.isComposing
					) {
						e.preventDefault();
						handleSend();
					}
				}}
				placeholder={placeholder}
				disabled={!canSendText}
				rows={1}
				className="min-h-11 max-h-[140px] flex-1 resize-none overflow-y-auto rounded-2xl border border-outline-variant bg-surface-container-low px-4 py-2.5 text-body-md leading-6 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
			/>
			<button
				type="button"
				onClick={handleSend}
				aria-label="전송"
				className="mb-1.5 text-primary disabled:opacity-40"
				disabled={!canSendText}
			>
				<Icon name="send" />
			</button>
		</div>
	);
}
