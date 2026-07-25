const nationalIdPattern = /^\d{13}$/;

export function nationalIdValidationMessage(nationalId: string) {
  if (!nationalIdPattern.test(nationalId)) {
    return "National ID must contain 13 digits.";
  }

  if (!isValidNationalId(nationalId)) {
    return "National ID checksum is invalid.";
  }

  return "";
}

export function isValidNationalId(nationalId: string) {
  if (!nationalIdPattern.test(nationalId)) {
    return false;
  }

  let sum = 0;
  for (let index = 0; index < 12; index += 1) {
    sum += Number(nationalId[index]) * (13 - index);
  }

  const expectedCheckDigit = (11 - (sum % 11)) % 10;
  return expectedCheckDigit === Number(nationalId[12]);
}