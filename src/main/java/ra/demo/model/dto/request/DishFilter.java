package ra.demo.model.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class DishFilter {
    @Size(max = 120, message = "{validation.name.length}")
    private String name;
    @Positive(message = "{validation.category.required}")
    private Long categoryId;
    private Boolean available;
    @DecimalMin(value = "0", message = "{validation.filter.price}")
    @Digits(integer = 10, fraction = 2, message = "{validation.price.digits}")
    private BigDecimal minPrice;
    @DecimalMin(value = "0", message = "{validation.filter.price}")
    @Digits(integer = 10, fraction = 2, message = "{validation.price.digits}")
    private BigDecimal maxPrice;
    @Pattern(regexp = "name|priceAsc|priceDesc|timeAsc", message = "{validation.sort}")
    private String sort = "name";
    @AssertTrue(message = "{error.price.range}")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
