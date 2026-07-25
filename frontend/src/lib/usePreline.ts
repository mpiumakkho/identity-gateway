import { useEffect } from "react";

export function usePreline() {
  useEffect(() => {
    void import("preline/non-auto").then(({ HSStaticMethods }) => {
      HSStaticMethods.autoInit();
    });
  }, []);
}