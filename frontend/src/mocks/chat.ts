export interface MockChatMessage {
	id: string;
	sender: "me" | "creator";
	text?: string;
	imageSeed?: string;
	sentAtLabel: string;
}

export const mockChatThread = {
	/** 로컬 테스트: DB member id=9 (이동준). 실제 채팅 API 연동 전 목업. */
	creator: { id: "9", name: "이동준", avatarSeed: "artisan-dongjun" },
	messages: [] as MockChatMessage[],
};
