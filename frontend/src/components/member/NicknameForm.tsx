import { useState } from "react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

interface NicknameFormProps {
	initialNickname: string;
}

export function NicknameForm({ initialNickname }: NicknameFormProps) {
	const [nickname, setNickname] = useState(initialNickname);

	function onSubmit(e: React.FormEvent) {
		e.preventDefault();

		// 커밋 4에서 API 연동
	}

	return (
		<form className="mb-5 flex max-w-md flex-col gap-5" onSubmit={onSubmit}>
			<Input
				label="닉네임"
				value={nickname}
				onChange={(e) => setNickname(e.target.value)}
			/>

			<Button type="submit">변경사항 저장</Button>
		</form>
	);
}