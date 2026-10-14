package ra.demo.exception;
import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private final String messageKey;
    private final String field;
    public BusinessException(String messageKey) { this(messageKey, null); }
    public BusinessException(String messageKey, String field) {
        super(messageKey);
        this.messageKey = messageKey;
        this.field = field;
    }
}
