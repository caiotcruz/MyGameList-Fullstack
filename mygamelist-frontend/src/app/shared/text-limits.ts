// UTF-16 code units: same semantics as HTML maxlength and Java @Size.
export const TEXT_LIMITS = Object.freeze({ REVIEW: 50000, BIO: 5000, COMMENT: 5000, MESSAGE: 10000, NAME: 80 });
export function reviewLimitError(text: string | null | undefined): string {
  return (text?.length ?? 0) > TEXT_LIMITS.REVIEW
    ? `A review pode ter até ${TEXT_LIMITS.REVIEW.toLocaleString('pt-BR')} caracteres. Seu texto foi preservado.` : '';
}
