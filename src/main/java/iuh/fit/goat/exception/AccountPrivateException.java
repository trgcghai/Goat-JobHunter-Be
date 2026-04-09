package iuh.fit.goat.exception;

public class AccountPrivateException extends InvalidException {
    public static final String ERROR_CODE = "ACCOUNT_PRIVATE";

    public AccountPrivateException(String message) {
        super(message);
    }
}
