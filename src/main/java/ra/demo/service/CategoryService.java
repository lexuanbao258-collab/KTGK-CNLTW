package ra.demo.service;
import ra.demo.model.dto.request.CategoryRequest;
import ra.demo.model.entity.DishCategory_BIT240038;
import java.util.List;
public interface CategoryService {
    List<DishCategory_BIT240038> list();
    CategoryRequest getForm(Long id);
    Long save(Long id, CategoryRequest request);
    void delete(Long id);
}
