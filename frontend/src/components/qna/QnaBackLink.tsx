import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Icon } from "@/components/ui/Icon";

interface QnaBackLinkProps {
	creatorId: string | number;
}

export function QnaBackLink({ creatorId }: QnaBackLinkProps) {
	return (
		<Link
			to={paths.creatorQna(creatorId)}
			className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
		>
			<Icon name="arrow_back" className="text-[18px]" />
			Q&A 게시판
		</Link>
	);
}
