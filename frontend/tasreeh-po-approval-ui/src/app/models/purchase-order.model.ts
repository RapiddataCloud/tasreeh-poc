export type PurchaseOrder = {
  id: string;
  item: string;
  amount: number;
  description: string;
  status: string;
  createdAt?: string;
  submittedBy?: string;
  approvedBy?: string;
  approvalType?: 'auto' | 'manual';
  reason?: string;
};
