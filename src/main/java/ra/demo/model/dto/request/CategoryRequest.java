package ra.demo.model.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class CategoryRequest {
    @NotBlank(message = "{validation.name.required}")
    @Size(max = 100, message = "{validation.category.length}")
    private String name;
    @Size(max = 2000, message = "{validation.description.length}")
    private String description;
}
