export interface FormatResult {
	value: string;
	cursor: number;
}

/** 선택 영역을 before/after로 감싼다. 선택이 없으면 placeholder를 대신 감싼다. */
export function wrapSelection(
	value: string,
	selectionStart: number,
	selectionEnd: number,
	before: string,
	after: string,
	placeholder: string,
): FormatResult {
	const selected = value.slice(selectionStart, selectionEnd) || placeholder;
	const nextValue = value.slice(0, selectionStart) + before + selected + after + value.slice(selectionEnd);
	return { value: nextValue, cursor: selectionStart + before.length + selected.length + after.length };
}

/** 선택 영역이 걸쳐 있는 모든 줄의 맨 앞에 prefix를 삽입한다(글머리 기호용). 선택이 없으면 커서가 있는 한 줄에만 적용된다. */
export function prefixCurrentLine(
	value: string,
	selectionStart: number,
	selectionEnd: number,
	prefix: string,
): FormatResult {
	const blockStart = value.lastIndexOf("\n", selectionStart - 1) + 1;
	const nextNewline = value.indexOf("\n", selectionEnd);
	const blockEnd = nextNewline === -1 ? value.length : nextNewline;

	const lines = value.slice(blockStart, blockEnd).split("\n");
	const nextBlock = lines.map((line) => prefix + line).join("\n");
	const nextValue = value.slice(0, blockStart) + nextBlock + value.slice(blockEnd);

	return { value: nextValue, cursor: selectionEnd + prefix.length * lines.length };
}

/** 선택 영역을 [텍스트](url) 링크로 감싼다. 선택이 없으면 placeholder 텍스트를 사용한다. */
export function insertLink(
	value: string,
	selectionStart: number,
	selectionEnd: number,
	url: string,
): FormatResult {
	const selected = value.slice(selectionStart, selectionEnd) || "링크 텍스트";
	const inserted = `[${selected}](${url})`;
	const nextValue = value.slice(0, selectionStart) + inserted + value.slice(selectionEnd);
	return { value: nextValue, cursor: selectionStart + inserted.length };
}

/** 포맷 적용 결과를 반영하고, 커서 위치를 복원한다(제어 컴포넌트라 리렌더 이후 프레임에서 복원). */
export function applyFormat(
	textarea: HTMLTextAreaElement,
	setValue: (v: string) => void,
	result: FormatResult,
) {
	setValue(result.value);
	requestAnimationFrame(() => {
		textarea.focus();
		textarea.setSelectionRange(result.cursor, result.cursor);
	});
}
