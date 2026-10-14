package ra.demo.model.dto.request;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class ImageRequest {
    @NotBlank(message = "{validation.image.required}")
    @Size(max = 2048, message = "{validation.image.length}")
    @URL(regexp = "^https?://.+", message = "{validation.image.url}")
    private String imageUrl;
    private boolean thumbnail;
}
