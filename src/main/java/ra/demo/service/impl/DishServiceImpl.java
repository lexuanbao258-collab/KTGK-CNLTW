package ra.demo.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.exception.*;
import ra.demo.model.dto.request.*;
import ra.demo.model.entity.*;
import ra.demo.repository.*;
import ra.demo.service.DishService;
import java.util.List;
import java.util.Objects;
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class DishServiceImpl implements DishService {
    private final DishRepository dishes;
    private final DishCategoryRepository categories;
    private final DishImageRepository images;
    public List<Dish_BIT240038> search(DishFilter filter) {
        if (!filter.isPriceRangeValid()) throw new BusinessException("error.price.range");
        Sort sort = switch (filter.getSort()) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "timeAsc" -> Sort.by("preparationTime").ascending();
            default -> Sort.by("name").ascending();
        };
        return dishes.findAll(DishSpecifications.matching(filter), sort.and(Sort.by("id")));
    }
    public Dish_BIT240038 get(Long id) {
        return dishes.findDetail(id).orElseThrow(() -> new ResourceNotFoundException("error.dish.notfound"));
    }
    public DishRequest getForm(Long id) {
        var dish = get(id);
        var form = new DishRequest();
        form.setName(dish.getName());
        form.setPrice(dish.getPrice());
        form.setPreparationTime(dish.getPreparationTime());
        form.setAvailable(dish.isAvailable());
        form.setDescription(dish.getDescription());
        form.setCategoryId(dish.getCategory().getId());
        return form;
    }
    @Transactional
    public Long save(Long id, DishRequest request) {
        var dish = id == null ? new Dish_BIT240038() : lock(id);
        var category = categories.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("error.category.notfound", "categoryId"));
        dishes.findByCategoryAndNormalizedName(category.getId(), request.getName()).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), id))
                throw new BusinessException("error.dish.duplicate", "name");
        });
        dish.setName(request.getName().trim());
        dish.setPrice(request.getPrice());
        dish.setPreparationTime(request.getPreparationTime());
        dish.setAvailable(request.isAvailable());
        dish.setDescription(request.getDescription());
        dish.setCategory(category);
        return dishes.saveAndFlush(dish).getId();
    }
    @Transactional
    public void delete(Long id) {
        dishes.delete(lock(id));
        dishes.flush();
    }
    @Transactional
    public void addImage(Long dishId, ImageRequest request) {
        var dish = lock(dishId);
        if (request.isThumbnail()) clearThumbnail(dish);
        var image = new DishImage_BIT240038();
        image.setDish(dish);
        image.setImageUrl(request.getImageUrl().trim());
        image.setThumbnail(request.isThumbnail());
        dish.getImages().add(image);
        images.saveAndFlush(image);
    }
    @Transactional
    public void selectThumbnail(Long dishId, Long imageId) {
        var dish = lock(dishId);
        var image = images.findByIdAndDishId(imageId, dishId)
                .orElseThrow(() -> new ResourceNotFoundException("error.image.notfound"));
        clearThumbnail(dish);
        image.setThumbnail(true);
        images.saveAndFlush(image);
    }
    private void clearThumbnail(Dish_BIT240038 dish) {
        for (var image : dish.getImages()) image.setThumbnail(false);
        // Release the unique thumbnail slot before saving the replacement.
        images.flush();
    }
    private Dish_BIT240038 lock(Long id) {
        return dishes.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("error.dish.notfound"));
    }
}
