package ra.demo.model.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "BIT240038_DishImages")
@Getter @Setter @NoArgsConstructor
public class DishImage_BIT240038 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIT240038_Id")
    private Long id;
    @Column(name = "BIT240038_ImageUrl", nullable = false, length = 2048)
    private String imageUrl;
    @Column(name = "BIT240038_IsThumbnail", nullable = false)
    private boolean thumbnail;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BIT240038_DishId", nullable = false)
    private Dish_BIT240038 dish;
}
