package ra.demo.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ra.demo.model.entity.DishImage_BIT240038;
import java.util.Optional;
public interface DishImageRepository extends JpaRepository<DishImage_BIT240038, Long> {
    Optional<DishImage_BIT240038> findByIdAndDishId(Long id, Long dishId);
}
