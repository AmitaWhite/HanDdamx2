/** 문자열이 maxLength 를 넘으면 잘라내고 "..." 을 붙인다. */
export function truncateText(value: string, maxLength: number): string {
	if (value.length <= maxLength) return value;
	return value.slice(0, maxLength) + "...";
}
