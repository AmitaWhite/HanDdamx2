import { cn } from "@/lib/cn";

interface AvatarProps {
	src?: string;
	alt?: string;
	/** px 크기 (기본 40) */
	size?: number;
	className?: string;
}

/** 원형 아바타. src 없으면 회색 플레이스홀더. */
export function Avatar({ src, alt = "", size = 40, className }: AvatarProps) {
	return (
		<span
			className={cn(
				"inline-block overflow-hidden rounded-full bg-surface-container",
				className,
			)}
			style={{ width: size, height: size }}
		>
			{src ? (
				<img src={src} alt={alt} className="h-full w-full object-cover" />
			) : null}
		</span>
	);
}
