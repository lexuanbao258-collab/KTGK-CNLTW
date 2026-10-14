package ra.demo.service;
import ra.demo.model.dto.request.*;
import ra.demo.model.entity.Dish_BIT240038;
import java.util.List;
public interface DishService {
    List<Dish_BIT240038> search(DishFilter filter);
    Dish_BIT240038 get(Long id);
    DishRequest getForm(Long id);
    Long save(Long id, DishRequest request);
    void delete(Long id);
    void addImage(Long dishId, ImageRequest request);
    void selectThumbnail(Long dishId, Long imageId);
}
