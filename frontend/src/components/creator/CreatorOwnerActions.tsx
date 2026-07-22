import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { CreatorPriceModal } from "@/components/creator/CreatorPriceModal";
import type { CreatorProfile } from "@/features/creator/types";

interface CreatorOwnerActionsProps {
  creator: CreatorProfile;
  onCreatorUpdated: (updated: CreatorProfile) => void;
}

// 내 크리에이터 홈(CreatorPage, isMine)에서만 보이는 관리 버튼
export function CreatorOwnerActions({ creator, onCreatorUpdated }: CreatorOwnerActionsProps) {
  const [showPriceModal, setShowPriceModal] = useState(false);

  return (
    <>
      <div className="flex shrink-0 flex-wrap justify-center gap-2">
        <Button variant="outline" onClick={() => setShowPriceModal(true)}>
          유료 구독 설정
        </Button>
      </div>

      <CreatorPriceModal
        open={showPriceModal}
        creator={creator}
        onClose={() => setShowPriceModal(false)}
        onSaved={onCreatorUpdated}
      />
    </>
  );
}
