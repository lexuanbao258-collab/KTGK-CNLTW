package ra.demo.exception;
import lombok.Getter;
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String messageKey;
    public ResourceNotFoundException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
