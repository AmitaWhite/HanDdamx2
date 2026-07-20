export interface MockChatMessage {
	id: string;
	sender: "me" | "creator";
	text?: string;
	imageSeed?: string;
	sentAtLabel: string;
}

export const mockChatThread = {
	creator: { id: "suyeon", name: "이수연", avatarSeed: "artisan-suyeon" },
	messages: [
		{ id: "m1", sender: "me", text: "안녕하세요! 지난번 리스 정말 예뻤어요 :)", sentAtLabel: "오후 1:58" },
		{ id: "m2", sender: "creator", text: "감사합니다! 다음 작품도 기대해주세요.", sentAtLabel: "오후 1:59" },
		{ id: "m3", sender: "me", text: "혹시 이 도안 파일도 받아볼 수 있을까요?", sentAtLabel: "오후 2:00" },
		{ id: "m4", sender: "creator", imageSeed: "chat-attach-1", sentAtLabel: "오후 2:01" },
		{ id: "m5", sender: "creator", text: "여기 도안 사진 보내드려요!", sentAtLabel: "오후 2:01" },
	] as MockChatMessage[],
};
