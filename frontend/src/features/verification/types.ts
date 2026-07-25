export type MethodId = "DIP_CHIP" | "MANUAL_ENTRY";

export type SessionOperator = {
  operatorId: string;
  username: string;
  displayName: string;
};

export type VerificationSession = {
  transactionId: string;
  method: MethodId;
  status: string;
  createdBy: SessionOperator | null;
  createdAt: string;
};

export type ManualIdentityPayload = {
  nationalId: string;
  title: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  laserCode: string;
};

export type ManualIdentityResult = {
  transactionId: string;
  sessionStatus: string;
  maskedNationalId: string;
  title: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  updatedAt: string;
};

export type DipChipPayload = ManualIdentityPayload & {
  cardIssueDate: string;
  cardExpiryDate: string;
  readerName: string;
  readerSerialNumber: string;
  rawPayload: string;
};

export type DipChipPayloadResult = {
  transactionId: string;
  sessionStatus: string;
  maskedNationalId: string;
  title: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  cardIssueDate: string;
  cardExpiryDate: string;
  readerName: string;
  readerSerialNumber: string;
  updatedAt: string;
};
export type DopaValidationPayload = {
  consentReference: string;
};

export type DopaValidationResult = {
  transactionId: string;
  sessionStatus: string;
  validationStatus: string;
  identitySource: string;
  maskedNationalId: string;
  responseCode: string;
  responseMessage: string;
  consentReference: string;
  validatedAt: string;
};
export type CloseVerificationPayload = {
  decision: "APPROVED" | "REJECTED";
  notes: string;
};

export type VerificationCloseoutResult = {
  transactionId: string;
  sessionStatus: string;
  decision: string;
  notes: string | null;
  decidedBy: SessionOperator;
  decidedAt: string;
};
