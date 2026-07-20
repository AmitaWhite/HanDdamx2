import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Icon } from "@/components/ui/Icon";
import { mockChatThread, type MockChatMessage } from "@/mocks/chat";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";

export function ChatPage() {
	const [searchParams] = useSearchParams();
	const creatorId = searchParams.get("creatorId") ?? mockChatThread.creator.id;
	const creator = findCreator(creatorId);

	const [messages, setMessages] = useState<MockChatMessage[]>([]);
	const [draft, setDraft] = useState("");

	function send() {
		if (!draft.trim()) return;
		setMessages((prev) => [
			...prev,
			{
				id: `local-${Date.now()}`,
				sender: "me",
				text: draft.trim(),
				sentAtLabel: "방금",
			},
		]);
		setDraft("");
	}

	return (
		<div className="flex h-[calc(100vh-72px)] flex-col">
			<div className="container-page flex items-center gap-3 border-b border-outline-variant/50 py-4">
				<Link to={paths.home} aria-label="뒤로가기" className="text-on-surface">
					<Icon name="arrow_back" />
				</Link>
				<Link to={paths.creator(creator.id)} className="flex items-center gap-3">
					<Avatar src={mockImg(creator.avatarSeed, 80, 80)} size={36} />
					<span className="text-label-md font-label-md text-on-surface">
						{creator.name}
					</span>
				</Link>
			</div>

			<div className="container-page flex-1 space-y-3 overflow-y-auto py-6">
				{messages.map((m) => (
					<div
						key={m.id}
						className={`flex ${m.sender === "me" ? "justify-end" : "justify-start"}`}
					>
						<div
							className={
								"max-w-[70%] rounded-2xl px-4 py-2.5 text-body-md " +
								(m.sender === "me"
									? "bg-primary text-on-primary"
									: "bg-surface-container-lowest text-on-surface")
							}
						>
							{m.imageSeed && (
								<img
									src={mockImg(m.imageSeed, 300, 300)}
									alt="첨부 이미지"
									className="mb-2 rounded-lg"
								/>
							)}
							{m.text}
						</div>
					</div>
				))}
			</div>

			<div className="container-page flex items-center gap-2 border-t border-outline-variant/50 py-4">
				<button type="button" aria-label="첨부" className="text-secondary">
					<Icon name="add_circle" />
				</button>
				<input
					value={draft}
					onChange={(e) => setDraft(e.target.value)}
					onKeyDown={(e) => e.key === "Enter" && send()}
					placeholder="메시지를 입력하세요"
					className="h-11 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
				/>
				<button type="button" onClick={send} aria-label="전송" className="text-primary">
					<Icon name="send" />
				</button>
			</div>
		</div>
	);
}
