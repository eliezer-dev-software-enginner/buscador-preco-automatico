package my_app;

public class CnpjValidator {

    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;

        // Remove caracteres especiais: . / -
        String cleaned = cnpj.replaceAll("[.\\-/]", "");

        // Verifica se tem exatamente 14 dígitos
        if (cleaned.length() != 14) return false;

        // Verifica se todos os caracteres são dígitos
        if (!cleaned.matches("\\d{14}")) return false;

        // Rejeita CNPJs com todos os dígitos iguais (ex: 00000000000000)
        if (cleaned.chars().distinct().count() == 1) return false;

        // Valida os dois dígitos verificadores
        return validateDigits(cleaned);
    }

    private static boolean validateDigits(String cnpj) {
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int firstDigit  = calcDigit(cnpj, weights1);
        int secondDigit = calcDigit(cnpj, weights2);

        return cnpj.charAt(12) - '0' == firstDigit
                && cnpj.charAt(13) - '0' == secondDigit;
    }

    private static int calcDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (cnpj.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    // Teste rápido
    public static void main(String[] args) {
        System.out.println(isValid("19.097.931/0001-88")); // true
        System.out.println(isValid("19.097.931/0001-8"));  // false
        System.out.println(isValid("11.111.111/1111-11")); // false (dígitos iguais)
        System.out.println(isValid(null));                  // false
    }
}