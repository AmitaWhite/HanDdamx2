import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Card } from "@/components/ui/Card";
import { mockImg } from "@/mocks/helpers";
import { cn } from "@/lib/cn";

interface PostCardProps {
	href: string;
	imageSeed: string;
	imageAlt: string;
	/** 이미지 위 절대 위치 뱃지(카테고리 칩·유료 칩 등). 위치 className은 호출부가 지정. */
	overlay?: ReactNode;
	children: ReactNode;
	className?: string;
}

/**
 * "정사각형 이미지(hover 확대) + 뱃지 오버레이" 카드 셸.
 * 이미지 아래 콘텐츠는 화면마다 조합이 달라 children으로 각자 구성한다.
 */
export function PostCard({ href, imageSeed, imageAlt, overlay, children, className }: PostCardProps) {
	return (
		<Link to={href}>
			<Card interactive className={cn("group overflow-hidden", className)}>
				<div className="relative aspect-square overflow-hidden">
					{overlay}
					<img
						src={mockImg(imageSeed)}
						alt={imageAlt}
						className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
					/>
				</div>
				{children}
			</Card>
		</Link>
	);
}
