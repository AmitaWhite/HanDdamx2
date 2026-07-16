import { cn } from "@/lib/cn";

interface IconProps {
	/** Material Symbols Outlined 아이콘 이름 (예: "search", "favorite") */
	name: string;
	className?: string;
	/** 접근성 이름. 주면 role="img"+aria-label, 없으면 장식용(aria-hidden). */
	label?: string;
}

/** Material Symbols Outlined 래퍼. 크기는 부모 font-size 를 따른다. */
export function Icon({ name, className, label }: IconProps) {
	// aria-label 은 role 이 있을 때만 유효 → label 유무로 속성 자체를 분기.
	const a11y = label
		? ({ role: "img", "aria-label": label } as const)
		: ({ "aria-hidden": true } as const);

	return (
		<span className={cn("material-symbols-outlined", className)} {...a11y}>
			{name}
		</span>
	);
}
