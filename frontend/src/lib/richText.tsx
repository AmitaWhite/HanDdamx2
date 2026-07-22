import type { ReactNode } from "react";

const INLINE_PATTERN = /(\*\*(.+?)\*\*)|(\*(.+?)\*)|(\[(.+?)\]\((https?:\/\/[^\s)]+)\))/g;

/** **볼드**, *이탤릭*, [텍스트](https://...) 문법을 인라인 React 노드로 변환한다. */
function renderInline(text: string): ReactNode[] {
	const nodes: ReactNode[] = [];
	let lastIndex = 0;
	let key = 0;
	let match: RegExpExecArray | null;
	INLINE_PATTERN.lastIndex = 0;
	while ((match = INLINE_PATTERN.exec(text))) {
		if (match.index > lastIndex) nodes.push(text.slice(lastIndex, match.index));
		if (match[2] !== undefined) {
			nodes.push(<strong key={key++}>{match[2]}</strong>);
		} else if (match[4] !== undefined) {
			nodes.push(<em key={key++}>{match[4]}</em>);
		} else if (match[6] !== undefined) {
			nodes.push(
				<a
					key={key++}
					href={match[7]}
					target="_blank"
					rel="noreferrer"
					className="text-primary underline"
				>
					{match[6]}
				</a>,
			);
		}
		lastIndex = INLINE_PATTERN.lastIndex;
	}
	if (lastIndex < text.length) nodes.push(text.slice(lastIndex));
	return nodes;
}

/**
 * 게시물 본문 렌더링 — content는 순수 텍스트로 저장되지만, 작성 툴바에서 넣은
 * **볼드**·*이탤릭*·`- ` 글머리 기호·[텍스트](url) 링크 문법을 여기서 해석해 보여준다.
 * dangerouslySetInnerHTML 없이 React 노드만 만들어서 XSS 우려가 없다.
 */
export function renderFormattedContent(content: string): ReactNode {
	const lines = content.split("\n");
	const blocks: ReactNode[] = [];
	let listBuffer: ReactNode[] = [];
	let key = 0;

	function flushList() {
		if (listBuffer.length > 0) {
			blocks.push(
				<ul key={`ul-${key++}`} className="my-1 list-disc pl-5">
					{listBuffer}
				</ul>,
			);
			listBuffer = [];
		}
	}

	for (const line of lines) {
		const isListItem = /^-\s+/.test(line);
		if (isListItem) {
			listBuffer.push(<li key={`li-${key++}`}>{renderInline(line.replace(/^-\s+/, ""))}</li>);
			continue;
		}
		flushList();
		if (line.trim() === "") {
			blocks.push(<br key={`br-${key++}`} />);
		} else {
			blocks.push(
				<p key={`p-${key++}`} className="whitespace-pre-wrap">
					{renderInline(line)}
				</p>,
			);
		}
	}
	flushList();
	return blocks;
}
