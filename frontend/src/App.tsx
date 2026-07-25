import { VerificationShell } from "./features/verification/VerificationShell";
import { usePreline } from "./lib/usePreline";

export default function App() {
  usePreline();

  return <VerificationShell />;
}