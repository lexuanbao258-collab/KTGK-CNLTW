package ra.demo.model.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "BIT240038_Dishes")
@Getter @Setter @NoArgsConstructor
public class Dish_BIT240038 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIT240038_Id")
    private Long id;
    @Column(name = "BIT240038_Name", nullable = false, length = 120)
    private String name;
    @Column(name = "BIT240038_Price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "BIT240038_PreparationTime", nullable = false)
    private Integer preparationTime;
    @Column(name = "BIT240038_IsAvailable", nullable = false)
    private boolean available;
    @Column(name = "BIT240038_Description", length = 2000)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BIT240038_DishCategoryId", nullable = false)
    private DishCategory_BIT240038 category;
    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DishImage_BIT240038> images = new ArrayList<>();
    public boolean isQuick() { return preparationTime <= 15; }
}
