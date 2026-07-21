import { useRef, useState } from "react";
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
 *   <li>Enter 로 텍스트 전송</li>
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
			: "메시지를 입력하세요";

	return (
		<div className="container-page flex items-center gap-2 border-t border-outline-variant/50 py-4">
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
				className="text-secondary disabled:opacity-40"
				disabled={!canSendImage}
				onClick={() => fileInputRef.current?.click()}
			>
				<Icon name={imageBusy ? "hourglass_empty" : "add_circle"} />
			</button>
			<input
				value={draft}
				onChange={(e) => setDraft(e.target.value)}
				onKeyDown={(e) => {
					if (e.key === "Enter") handleSend();
				}}
				placeholder={placeholder}
				disabled={!canSendText}
				className="h-11 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
			/>
			<button
				type="button"
				onClick={handleSend}
				aria-label="전송"
				className="text-primary disabled:opacity-40"
				disabled={!canSendText}
			>
				<Icon name="send" />
			</button>
		</div>
	);
}
