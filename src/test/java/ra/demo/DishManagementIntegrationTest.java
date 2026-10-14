package ra.demo;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.config.SampleDataInitializer;
import ra.demo.exception.*;
import ra.demo.model.dto.request.*;
import ra.demo.model.entity.*;
import ra.demo.repository.*;
import ra.demo.service.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class DishManagementIntegrationTest {
    @Autowired DishService dishes;
    @Autowired CategoryService categories;
    @Autowired DishRepository dishRepository;
    @Autowired DishCategoryRepository categoryRepository;
    @Autowired DishImageRepository imageRepository;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired Validator validator;
    @Autowired SampleDataInitializer seed;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        boolean postgres = "postgres".equals(System.getenv("TEST_DATABASE"));
        String url = postgres ? System.getenv("TEST_DB_URL") : "jdbc:h2:mem:dishes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        if (postgres && (url == null || !url.endsWith("/BIT240038_MID_TEST")))
            throw new IllegalStateException("TEST_DB_URL must point to the separate BIT240038_MID_TEST database");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> postgres ? "postgres" : "sa");
        registry.add("spring.datasource.password", () -> postgres ? Objects.requireNonNull(System.getenv("TEST_DB_PASSWORD")) : "");
        registry.add("spring.sql.init.schema-locations", () -> postgres ? "classpath:schema.sql" : "classpath:schema-h2.sql");
    }
    private Long category(String name) {
        var form = new CategoryRequest(); form.setName(name);
        return categories.save(null, form);
    }
    private DishRequest dishForm(String name, String price, int time, boolean available, Long categoryId) {
        var form = new DishRequest();
        form.setName(name); form.setPrice(new BigDecimal(price));
        form.setPreparationTime(time); form.setAvailable(available); form.setCategoryId(categoryId);
        return form;
    }
    private Long dish(String name, String price, int time, boolean available, Long categoryId) {
        return dishes.save(null, dishForm(name, price, time, available, categoryId));
    }
    private ImageRequest image(String url, boolean thumbnail) {
        var form = new ImageRequest(); form.setImageUrl(url); form.setThumbnail(thumbnail);
        return form;
    }
    @Test
    void createEditAndDeleteDishIncludingImages() {
        Long c = category("CRUD category");
        Long id = dish("CRUD dish", "12.50", 15, false, c);
        assertThat(dishes.get(id).isQuick()).isTrue();
        assertThat(dishes.get(id).isAvailable()).isFalse();
        dishes.save(id, dishForm("Edited dish", "20.50", 16, true, c));
        assertThat(dishes.get(id).isQuick()).isFalse();
        assertThat(dishes.get(id).getName()).isEqualTo("Edited dish");
        dishes.addImage(id, image("https://example.com/one.jpg", false));
        Long imageId = dishes.get(id).getImages().getFirst().getId();
        dishes.delete(id);
        assertThat(imageRepository.existsById(imageId)).isFalse();
        assertThatThrownBy(() -> dishes.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void duplicateNameIsNormalizedWithinCategoryAndEditCanKeepItsOwnName() {
        Long c = category("Duplicate category");
        Long id = dish("Soup", "20", 15, true, c);
        dishes.save(id, dishForm(" soup ", "21", 15, true, c));
        assertThatThrownBy(() -> dish("SOUP", "30", 16, true, c))
                .isInstanceOf(BusinessException.class).hasMessage("error.dish.duplicate");
        assertThatCode(() -> dish("Soup", "30", 16, true, category("Another category"))).doesNotThrowAnyException();
    }
    @Test
    void filterCombinesAllConditionsAtDatabaseAndIncludesUnavailable() {
        Long c = category("Filter category");
        Long matching = dish("Special soup", "40", 15, false, c);
        dish("Special rice", "60", 20, false, c);
        dish("Special soup", "40", 15, true, category("Filter other"));
        var filter = new DishFilter();
        filter.setName("soup"); filter.setCategoryId(c); filter.setAvailable(false);
        filter.setMinPrice(new BigDecimal("35")); filter.setMaxPrice(new BigDecimal("45"));
        assertThat(dishes.search(filter)).extracting(Dish_BIT240038::getId).containsExactly(matching);
    }
    @Test
    void sortPriceAndPreparationTimeAtDatabase() {
        Long c = category("Sort category");
        Long a = dish("A", "60", 30, true, c), b = dish("B", "20", 10, true, c), d = dish("C", "40", 20, true, c);
        var filter = new DishFilter(); filter.setCategoryId(c);
        filter.setSort("priceAsc");
        assertThat(dishes.search(filter)).extracting(Dish_BIT240038::getId).containsExactly(b,d,a);
        filter.setSort("priceDesc");
        assertThat(dishes.search(filter)).extracting(Dish_BIT240038::getId).containsExactly(a,d,b);
        filter.setSort("timeAsc");
        assertThat(dishes.search(filter)).extracting(Dish_BIT240038::getId).containsExactly(b,d,a);
    }
    @Test
    void searchTreatsPercentAndUnderscoreAsLiteralCharacters() {
        Long c = category("Literal category");
        Long id = dish("100%_soup", "20", 10, true, c);
        dish("100 other soup", "20", 10, true, c);
        var filter = new DishFilter(); filter.setCategoryId(c); filter.setName("%_");
        assertThat(dishes.search(filter)).extracting(Dish_BIT240038::getId).containsExactly(id);
    }
    @Test
    void invalidPriceRangeAndCategoryAreRejected() throws Exception {
        var filter = new DishFilter(); filter.setMinPrice(new BigDecimal("50")); filter.setMaxPrice(new BigDecimal("10"));
        assertThat(validator.validate(filter)).isNotEmpty();
        assertThatThrownBy(() -> dishes.search(filter)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> dish("Missing category", "10", 10, true, Long.MAX_VALUE))
                .isInstanceOf(BusinessException.class).hasMessage("error.category.notfound");
        mvc.perform(get("/dishes").param("minPrice","50").param("maxPrice","10"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Minimum price must not exceed maximum price.")));
    }
    @Test
    void validationRejectsBlankNameNonpositivePriceTimeAndBadUrl() {
        var form = dishForm(" ", "0", 0, true, null);
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString())
                .contains("name","price","preparationTime","categoryId");
        assertThat(validator.validate(image("javascript:alert(1)", false))).hasSize(1);
        assertThat(validator.validate(image("ftp://example.com/image.jpg", false))).hasSize(1);
        assertThat(validator.validate(image("https://example.com/image.jpg", false))).isEmpty();
        form = dishForm("Soup", "10.123", 1, true, 1L);
        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString()).contains("price");
    }
    @Test
    void categoryCrudAndUsedCategoryCannotBeDeleted() {
        Long c = category("Unused category");
        var request = new CategoryRequest(); request.setName("Edited category"); request.setDescription("Description");
        categories.save(c, request);
        assertThat(categories.getForm(c).getName()).isEqualTo("Edited category");
        categories.delete(c);
        assertThatThrownBy(() -> categories.getForm(c)).isInstanceOf(ResourceNotFoundException.class);
        Long used = category("Used category");
        dish("Used dish", "20", 15, true, used);
        assertThatThrownBy(() -> categories.delete(used)).isInstanceOf(BusinessException.class).hasMessage("error.category.used");
    }
    @Test
    void addMultipleImagesAndChangeThumbnail() {
        Long id = dish("Image dish", "20", 15, true, category("Image category"));
        dishes.addImage(id, image("https://example.com/1.jpg", true));
        dishes.addImage(id, image("https://example.com/2.jpg", true));
        var detail = dishes.get(id);
        assertThat(detail.getImages()).hasSize(2);
        assertThat(detail.getImages()).filteredOn(DishImage_BIT240038::isThumbnail).hasSize(1);
        Long first = detail.getImages().getFirst().getId();
        dishes.selectThumbnail(id, first);
        assertThat(dishes.get(id).getImages()).filteredOn(DishImage_BIT240038::isThumbnail)
                .extracting(DishImage_BIT240038::getId).containsExactly(first);
        dishes.selectThumbnail(id, first);
        assertThat(dishes.get(id).getImages()).filteredOn(DishImage_BIT240038::isThumbnail).hasSize(1);
    }
    @Test
    void thumbnailCannotUseAnImageFromAnotherDishAndMissingImagePreservesOldThumbnail() {
        Long c = category("Ownership category");
        Long a = dish("Ownership A", "20", 15, true, c), b = dish("Ownership B", "20", 15, true, c);
        dishes.addImage(a, image("https://example.com/a.jpg", true));
        dishes.addImage(b, image("https://example.com/b.jpg", true));
        Long wrong = dishes.get(b).getImages().getFirst().getId();
        assertThatThrownBy(() -> dishes.selectThumbnail(a, wrong)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> dishes.selectThumbnail(a, Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(dishes.get(a).getImages().getFirst().isThumbnail()).isTrue();
    }
    @Test
    void firstVisitIsEnglishDespiteBrowserHeaderAndVietnameseCookiePersists() throws Exception {
        mvc.perform(get("/dishes").header("Accept-Language","vi-VN,vi;q=0.9"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Kitchen dishes")))
                .andExpect(content().string(containsString("lang=\"en\"")));
        var response = mvc.perform(get("/dishes").param("lang","vi"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Danh sách món ăn")))
                .andReturn().getResponse();
        Cookie locale = response.getCookie("dish-language");
        assertThat(locale).isNotNull();
        mvc.perform(get("/categories").cookie(locale))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Danh sách loại món")));
        mvc.perform(get("/dishes/new").cookie(locale))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Thêm món ăn")));
    }
    @Test
    void languageLinksPreserveEveryFilterAndSortIncludingEncodedText() throws Exception {
        mvc.perform(get("/dishes?name=beef&categoryId=1&available=false&minPrice=10&maxPrice=100&sort=priceDesc&lang=vi"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=beef&amp;categoryId=1&amp;available=false&amp;minPrice=10&amp;maxPrice=100&amp;sort=priceDesc&amp;lang=en")))
                .andExpect(content().string(containsString("value=\"priceDesc\" selected=\"selected\"")));
    }
    @Test
    void formValidationIsTranslatedAndDoesNotPersistInvalidDish() throws Exception {
        long before = dishRepository.count();
        mvc.perform(post("/dishes").param("lang","vi").param("name","").param("price","0").param("preparationTime","0"))
                .andExpect(status().isOk()).andExpect(view().name("dishes/form"))
                .andExpect(content().string(containsString("/dishes/new?lang=en")))
                .andExpect(content().string(containsString("Tên không được để trống.")))
                .andExpect(content().string(containsString("Giá phải lớn hơn 0.")))
                .andExpect(content().string(containsString("Thời gian chế biến phải lớn hơn 0.")));
        assertThat(dishRepository.count()).isEqualTo(before);
        mvc.perform(post("/dishes").param("name","Soup").param("price","abc").param("preparationTime","abc"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Enter a valid price.")));
    }
    @Test
    void mvcCreateEditDetailAndDuplicateFormRender() throws Exception {
        Long c = category("MVC category");
        String location = mvc.perform(post("/dishes").param("name","MVC dish").param("price","50")
                .param("preparationTime","15").param("available","false").param("categoryId",c.toString()))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(location).isNotNull();
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(content().string(containsString("Quick")))
                .andExpect(content().string(containsString("Unavailable"))).andExpect(content().string(containsString("This dish has no images.")));
        Long id = dishRepository.findByCategoryAndNormalizedName(c,"MVC dish").orElseThrow().getId();
        mvc.perform(get(location+"/edit")).andExpect(status().isOk());
        mvc.perform(post(location).param("name","Edited MVC").param("price","75").param("preparationTime","16").param("categoryId",c.toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(location).param("lang","vi")).andExpect(status().isOk()).andExpect(content().string(containsString("Thông thường")));
        mvc.perform(post("/dishes").param("name","Edited MVC").param("price","25").param("preparationTime","12").param("categoryId",c.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("A dish with this name already exists in this category.")));
        assertThat(dishes.get(id).getPrice()).isEqualByComparingTo("75");
    }
    @Test
    void mvcImagesAndTranslatedFlashCategoryErrors() throws Exception {
        Long c = category("MVC image category"), id = dish("MVC image dish","25",12,true,c);
        mvc.perform(post("/dishes/"+id+"/images").param("imageUrl","bad-url").param("lang","vi"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Hãy nhập đường dẫn ảnh HTTP hoặc HTTPS hợp lệ.")))
                .andExpect(content().string(containsString("/dishes/"+id+"?lang=en")));
        mvc.perform(post("/dishes/"+id+"/images").param("imageUrl","https://example.com/1.jpg").param("thumbnail","true"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("successKey","flash.image.added"));
        mvc.perform(get("/dishes/"+id)).andExpect(status().isOk()).andExpect(content().string(containsString("https://example.com/1.jpg")));
        var flash = mvc.perform(post("/categories/"+c+"/delete")).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorKey","error.category.used")).andReturn().getFlashMap();
        mvc.perform(get("/categories").param("lang","vi").flashAttrs(flash))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không thể xóa loại món đang có món ăn.")));
    }
    @Test
    void missingIdentifiersReturnFriendlyErrorsWithoutSqlOrStackTrace() throws Exception {
        mvc.perform(get("/dishes/"+Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This dish does not exist.")));
        mvc.perform(get("/dishes/no-id")).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("The request contains an invalid value.")));
        mvc.perform(get("/missing-page")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This page does not exist.")));
        mvc.perform(get("/dishes").param("name","a name with no result"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("No dishes match these filters.")));
    }
    @Test
    void sampleDataIsIdempotentAndHasRequiredVariety() throws Exception {
        long beforeCategories = categoryRepository.count(), beforeDishes = dishRepository.count();
        seed.run(new DefaultApplicationArguments());
        seed.run(new DefaultApplicationArguments());
        assertThat(categoryRepository.count()).isEqualTo(beforeCategories);
        assertThat(dishRepository.count()).isEqualTo(beforeDishes);
        assertThat(beforeCategories).isGreaterThanOrEqualTo(3);
        assertThat(beforeDishes).isGreaterThanOrEqualTo(5);
        var filter = new DishFilter(); filter.setAvailable(false);
        assertThat(dishes.search(filter)).isNotEmpty();
    }
    @Test
    void actualSchemaContainsOnlyExactRequiredColumnsAndRelations() {
        var expected = Map.of(
            "BIT240038_DishCategories", Set.of("BIT240038_Id","BIT240038_Name","BIT240038_Description"),
            "BIT240038_Dishes", Set.of("BIT240038_Id","BIT240038_Name","BIT240038_Price","BIT240038_PreparationTime","BIT240038_IsAvailable","BIT240038_Description","BIT240038_DishCategoryId"),
            "BIT240038_DishImages", Set.of("BIT240038_Id","BIT240038_ImageUrl","BIT240038_IsThumbnail","BIT240038_DishId"));
        expected.forEach((table,columns) -> {
            var actual = jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name = ?", String.class, table);
            assertThat(actual).containsExactlyInAnyOrderElementsOf(columns);
        });
        assertThat(DishCategory_BIT240038.class.getSimpleName()).isEqualTo("DishCategory_BIT240038");
        assertThat(Dish_BIT240038.class.getSimpleName()).isEqualTo("Dish_BIT240038");
        assertThat(DishImage_BIT240038.class.getSimpleName()).isEqualTo("DishImage_BIT240038");
        var fkCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_type='FOREIGN KEY' AND table_name IN ('BIT240038_Dishes','BIT240038_DishImages')", Integer.class);
        assertThat(fkCount).isEqualTo(2);
    }
    @Test @EnabledIfEnvironmentVariable(named="TEST_DATABASE", matches="postgres")
    void postgresRejectsTwoThumbnailsEvenWithDirectSql() {
        Long id = dish("DB thumbnail dish","25",12,true,category("DB thumbnail category"));
        dishes.addImage(id,image("https://example.com/first.jpg",true));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO \"BIT240038_DishImages\" (\"BIT240038_ImageUrl\",\"BIT240038_IsThumbnail\",\"BIT240038_DishId\") VALUES (?,true,?)","https://example.com/second.jpg",id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test @EnabledIfEnvironmentVariable(named="TEST_DATABASE", matches="postgres")
    void postgresRejectsNormalizedDuplicateEvenWithDirectSql() {
        Long c = category("DB unique category");
        dish("DB unique dish","25",12,true,c);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO \"BIT240038_Dishes\" (\"BIT240038_Name\",\"BIT240038_Price\",\"BIT240038_PreparationTime\",\"BIT240038_IsAvailable\",\"BIT240038_DishCategoryId\") VALUES (?,25,12,true,?)"," DB UNIQUE DISH ",c))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
