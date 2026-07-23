import { forwardRef } from "react";
import { Link, type LinkProps } from "react-router-dom";
import { type ButtonSize, type ButtonVariant, buttonClassName } from "./Button";

interface LinkButtonProps extends LinkProps {
	variant?: ButtonVariant;
	size?: ButtonSize;
	fullWidth?: boolean;
}

/** Link를 Button과 동일한 스타일로 렌더링. <Link><Button/></Link> 중첩(a>button)을 피하기 위함. */
export const LinkButton = forwardRef<HTMLAnchorElement, LinkButtonProps>(
	({ variant, size, fullWidth, className, ...props }, ref) => (
		<Link
			ref={ref}
			className={buttonClassName({ variant, size, fullWidth, className })}
			{...props}
		/>
	),
);
LinkButton.displayName = "LinkButton";
