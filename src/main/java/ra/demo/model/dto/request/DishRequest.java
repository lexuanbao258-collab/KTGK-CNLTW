package ra.demo.model.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class DishRequest {
    @NotBlank(message = "{validation.name.required}")
    @Size(max = 120, message = "{validation.name.length}")
    private String name;
    @NotNull(message = "{validation.price.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{validation.price.positive}")
    @Digits(integer = 10, fraction = 2, message = "{validation.price.digits}")
    private BigDecimal price;
    @NotNull(message = "{validation.time.required}")
    @Positive(message = "{validation.time.positive}")
    private Integer preparationTime;
    private boolean available = true;
    @Size(max = 2000, message = "{validation.description.length}")
    private String description;
    @NotNull(message = "{validation.category.required}")
    @Positive(message = "{validation.category.required}")
    private Long categoryId;
}
