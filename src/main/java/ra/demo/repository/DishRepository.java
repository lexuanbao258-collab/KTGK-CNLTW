package ra.demo.repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ra.demo.model.entity.Dish_BIT240038;
import java.util.*;
public interface DishRepository extends JpaRepository<Dish_BIT240038, Long>, JpaSpecificationExecutor<Dish_BIT240038> {
    @Override @EntityGraph(attributePaths = "category")
    List<Dish_BIT240038> findAll(Specification<Dish_BIT240038> specification, Sort sort);
    @EntityGraph(attributePaths = {"category", "images"})
    @Query("select d from Dish_BIT240038 d where d.id = :id")
    Optional<Dish_BIT240038> findDetail(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dish_BIT240038 d where d.id = :id")
    Optional<Dish_BIT240038> findForUpdate(@Param("id") Long id);
    boolean existsByCategoryId(Long categoryId);
    @Query("select d from Dish_BIT240038 d where d.category.id = :categoryId and lower(trim(d.name)) = lower(trim(:name))")
    Optional<Dish_BIT240038> findByCategoryAndNormalizedName(@Param("categoryId") Long categoryId, @Param("name") String name);
}
