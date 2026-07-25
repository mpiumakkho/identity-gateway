import type { OperatorRole } from "../auth/types";

export type OperatorUser = {
  operatorId: string;
  username: string;
  displayName: string;
  role: OperatorRole;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  disabledAt: string | null;
};

export type CreateOperatorPayload = {
  username: string;
  displayName: string;
  password: string;
  role: OperatorRole;
};
