package ra.demo.model.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "BIT240038_DishCategories")
@Getter @Setter @NoArgsConstructor
public class DishCategory_BIT240038 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIT240038_Id")
    private Long id;
    @Column(name = "BIT240038_Name", nullable = false, length = 100)
    private String name;
    @Column(name = "BIT240038_Description", length = 2000)
    private String description;
    @OneToMany(mappedBy = "category")
    private List<Dish_BIT240038> dishes = new ArrayList<>();
}
