package ra.demo.repository;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import ra.demo.model.dto.request.DishFilter;
import ra.demo.model.entity.Dish_BIT240038;
import java.util.ArrayList;
import java.util.Locale;
public final class DishSpecifications {
    private DishSpecifications() {}
    public static Specification<Dish_BIT240038> matching(DishFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.getName() != null && !filter.getName().isBlank()) {
                String term = filter.getName().trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + term + "%", '\\'));
            }
            if (filter.getCategoryId() != null)
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            if (filter.getAvailable() != null)
                predicates.add(cb.equal(root.get("available"), filter.getAvailable()));
            if (filter.getMinPrice() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            if (filter.getMaxPrice() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
