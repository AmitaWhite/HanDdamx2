import { cn } from "@/lib/cn";

interface AvatarProps {
	src?: string;
	alt?: string;
	/** px 크기 (기본 40) */
	size?: number;
	/** src 없을 때 대신 표시할 이니셜의 원본 문자열*/
	fallbackText?: string;
	className?: string;
}

/** 원형 아바타. src 없으면 fallbackText 첫 글자, 그것도 없으면 회색 플레이스홀더. */
export function Avatar({ src, alt = "", size = 40, fallbackText, className }: AvatarProps) {
	const initial = !src ? fallbackText?.trim().charAt(0).toUpperCase() : undefined;

	return (
		<span
			className={cn(
				"inline-flex items-center justify-center overflow-hidden rounded-full",
				initial ? "bg-primary/10" : "bg-surface-container",
				className,
			)}
			style={{ width: size, height: size }}
		>
			{src ? (
				<img src={src} alt={alt} className="h-full w-full object-cover" />
			) : initial ? (
				<span
					className="font-display font-bold text-primary"
					style={{ fontSize: size * 0.42 }}
					aria-hidden="true"
				>
					{initial}
				</span>
			) : null}
		</span>
	);
}
