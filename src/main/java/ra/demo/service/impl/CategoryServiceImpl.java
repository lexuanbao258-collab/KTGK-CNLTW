package ra.demo.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.exception.*;
import ra.demo.model.dto.request.CategoryRequest;
import ra.demo.model.entity.DishCategory_BIT240038;
import ra.demo.repository.*;
import ra.demo.service.CategoryService;
import java.util.List;
import java.util.Objects;
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final DishCategoryRepository categories;
    private final DishRepository dishes;
    public List<DishCategory_BIT240038> list() {
        return categories.findAll(Sort.by("name").and(Sort.by("id")));
    }
    public CategoryRequest getForm(Long id) {
        var category = get(id);
        var form = new CategoryRequest();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        return form;
    }
    @Transactional
    public Long save(Long id, CategoryRequest request) {
        var category = id == null ? new DishCategory_BIT240038() : get(id);
        categories.findByNormalizedName(request.getName()).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), id))
                throw new BusinessException("error.category.duplicate", "name");
        });
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        return categories.saveAndFlush(category).getId();
    }
    @Transactional
    public void delete(Long id) {
        var category = get(id);
        if (dishes.existsByCategoryId(id)) throw new BusinessException("error.category.used");
        categories.delete(category);
        categories.flush();
    }
    private DishCategory_BIT240038 get(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("error.category.notfound"));
    }
}
