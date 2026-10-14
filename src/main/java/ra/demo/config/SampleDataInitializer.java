package ra.demo.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.model.entity.*;
import ra.demo.repository.*;
import java.math.BigDecimal;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SampleDataInitializer implements ApplicationRunner {
    private final DishCategoryRepository categories;
    private final DishRepository dishes;
    private final DishImageRepository images;
    @Override @Transactional
    public void run(ApplicationArguments args) {
        var starters = category("Starters", "Small dishes to begin your meal.");
        var mains = category("Main courses", "Complete dishes from the kitchen.");
        var desserts = category("Desserts", "A sweet finish.");
        dish("Fresh spring rolls", "35000", 10, true, starters);
        dish("Chicken salad", "65000", 15, true, starters);
        dish("Beef pho", "75000", 25, true, mains);
        dish("Grilled salmon", "180000", 30, false, mains);
        dish("Mango pudding", "45000", 12, true, desserts);
    }
    private DishCategory_BIT240038 category(String name, String description) {
        return categories.findByNormalizedName(name).orElseGet(() -> {
            var category = new DishCategory_BIT240038();
            category.setName(name);
            category.setDescription(description);
            return categories.save(category);
        });
    }
    private void dish(String name, String price, int time, boolean available, DishCategory_BIT240038 category) {
        if (dishes.findByCategoryAndNormalizedName(category.getId(), name).isPresent()) return;
        var dish = new Dish_BIT240038();
        dish.setName(name);
        dish.setPrice(new BigDecimal(price));
        dish.setPreparationTime(time);
        dish.setAvailable(available);
        dish.setCategory(category);
        dishes.save(dish);
        var image = new DishImage_BIT240038();
        image.setDish(dish);
        image.setImageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=800&auto=format&fit=crop");
        image.setThumbnail(true);
        images.save(image);
    }
}
