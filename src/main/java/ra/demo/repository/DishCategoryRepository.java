package ra.demo.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ra.demo.model.entity.DishCategory_BIT240038;
import java.util.Optional;
public interface DishCategoryRepository extends JpaRepository<DishCategory_BIT240038, Long> {
    @Query("select c from DishCategory_BIT240038 c where lower(trim(c.name)) = lower(trim(:name))")
    Optional<DishCategory_BIT240038> findByNormalizedName(@Param("name") String name);
}
