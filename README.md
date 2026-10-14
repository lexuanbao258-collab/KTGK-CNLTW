# Quản lý món ăn — BIT240038

Website giữa kỳ Spring MVC + Thymeleaf, Java 21, Spring Boot 3.5.16, Gradle 8.14.3, PostgreSQL, Lombok và Jakarta Validation. Không dùng Maven, Security/JWT hoặc frontend riêng.

## Cấu trúc

```text
src/main/java/ra/demo/
  DishManagementApplication.java
  controller/        DishController, CategoryController
  service/           DishService, CategoryService
  service/impl/      DishServiceImpl, CategoryServiceImpl
  repository/        DishRepository, DishCategoryRepository, DishImageRepository, DishSpecifications
  model/entity/      DishCategory_BIT240038, Dish_BIT240038, DishImage_BIT240038
  model/dto/request/ DishRequest, CategoryRequest, ImageRequest, DishFilter
  advice/            GlobalExceptionHandler
  exception/         BusinessException, ResourceNotFoundException
  config/            WebConfig, SampleDataInitializer
src/main/resources/
  application.properties, schema.sql
  messages.properties, messages_vi.properties
  templates/         dishes/, categories/, fragments/, error.html
  static/            css/, js/
src/test/            kiểm thử tích hợp, schema H2 riêng
sql/                 tạo database, kiểm tra schema PostgreSQL
```

Danh sách file đầy đủ: chạy `git ls-files` nếu đưa project vào Git, hoặc
`Get-ChildItem src,sql -File -Recurse | Select-Object -ExpandProperty FullName`.
Không cần tạo DTO response/common khi view MVC chưa có nhu cầu.

## Entity và database

- `DishCategory_BIT240038`: loại món; có nhiều món. Không cascade xóa món.
- `Dish_BIT240038`: món ăn; thuộc đúng một loại; có nhiều ảnh. Xóa món sẽ xóa ảnh của chính món đó trong transaction.
- `DishImage_BIT240038`: ảnh URL; thuộc đúng một món. Tối đa một thumbnail/món.
- Quan hệ `@OneToMany(mappedBy=...)` và `@ManyToOne(fetch=LAZY)`; khóa ngoại tường minh, không sinh bảng nối.
- Quick/Nhanh khi thời gian ≤ 15; Regular/Thông thường khi > 15. Tính trong Java, không có cột bổ sung.

| Bảng | Cột vật lý |
| --- | --- |
| BIT240038_DishCategories | BIT240038_Id, BIT240038_Name, BIT240038_Description |
| BIT240038_Dishes | BIT240038_Id, BIT240038_Name, BIT240038_Price, BIT240038_PreparationTime, BIT240038_IsAvailable, BIT240038_Description, BIT240038_DishCategoryId |
| BIT240038_DishImages | BIT240038_Id, BIT240038_ImageUrl, BIT240038_IsThumbnail, BIT240038_DishId |

Giữ đúng hoa/thường bằng `PhysicalNamingStrategyStandardImpl` và
`hibernate.globally_quoted_identifiers=true`, theo [hướng dẫn Hibernate](https://docs.jboss.org/hibernate/orm/6.6/javadocs/org/hibernate/cfg/MappingSettings.html).
`schema.sql` dùng identifier trong dấu nháy kép, chỉ CREATE TABLE/INDEX IF NOT EXISTS; Hibernate `ddl-auto=validate`. Không DROP, không create-drop, không xóa dữ liệu khi khởi động. Schema có NOT NULL, CHECK, FK, unique tên không phân biệt hoa/thường trong cùng loại và partial unique index thumbnail.
Schema có sẵn không tương thích sẽ làm khởi động thất bại để tránh tự sửa dữ liệu. Hãy kiểm tra và sao lưu trước khi tự điều chỉnh schema.

## Chạy trong IntelliJ

1. Mở thư mục chứa `settings.gradle`, import Gradle. Chọn Project SDK và Gradle JVM là Java 21. Dùng Gradle Wrapper.
2. PostgreSQL phải chạy tại localhost:5432. Trong pgAdmin Query Tool kết nối database `postgres`, chạy một lần:

```sql
CREATE DATABASE "BIT240038_MID";
```

Nếu database đã tồn tại thì tái sử dụng, không chạy lại CREATE DATABASE và không xóa database.
3. Trong Run Configuration của `DishManagementApplication`, đặt Environment variables `DB_PASSWORD` bằng mật khẩu tài khoản `postgres`. Không ghi mật khẩu vào source hoặc Git.
4. Chạy `DishManagementApplication`. Mở [http://localhost:8080/dishes](http://localhost:8080/dishes).
5. Kết nối `BIT240038_MID` trong pgAdmin để kiểm tra dữ liệu.

Java 21 tương thích Spring Boot 3.5 và Gradle đã chọn: [Spring Boot system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

## Chạy PowerShell / Gradle

Nhập mật khẩu trong cửa sổ PowerShell, không đưa mật khẩu vào lệnh hoặc file:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$dbCredential = Get-Credential -UserName postgres -Message 'Nhập mật khẩu PostgreSQL'
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
.\gradlew.bat bootRun
```

Có thể dùng `java -jar build\libs\DishManagement_BIT240038-1.0.0.jar` sau khi build, trong môi trường đã có DB_PASSWORD.

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

Test mặc định dùng H2 in-memory, không kết nối database chính.
Để kiểm thử trên PostgreSQL thật, tạo **database riêng** `"BIT240038_MID_TEST"`, không đưa URL database chính vào test:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/BIT240038_MID_TEST'
$testCredential = Get-Credential -UserName postgres -Message 'Mật khẩu PostgreSQL kiểm thử'
$env:TEST_DB_PASSWORD = $testCredential.GetNetworkCredential().Password
.\gradlew.bat postgresTest
```

Các test nghiệp vụ rollback dữ liệu test. Seeder chỉ bổ sung món mẫu chưa tồn tại; chạy lại không nhân đôi, không ghi đè món đã sửa. Tắt dữ liệu mẫu khi cần bằng `APP_SEED_ENABLED=false`.
Báo cáo: `build/reports/tests/test/index.html` và `build/reports/tests/postgresTest/index.html`.

## Nghiệp vụ và i18n

- CRUD món và loại món; dropdown loại lấy từ database.
- Validate tên, giá > 0 (tối đa 2 số lẻ), thời gian > 0, loại tồn tại, tên món không trùng trong cùng loại.
- Lọc đồng thời tên, loại, trạng thái, min/max; sort giá tăng/giảm hoặc thời gian tăng bằng SQL Specification. Tên chứa % hoặc _ được tìm như ký tự thường.
- Món ngừng bán vẫn hiện trong quản trị; min > max hiển thị lỗi ngay trên danh sách.
- Chi tiết hiển thị mọi ảnh; thêm URL HTTP/HTTPS; đổi thumbnail trong transaction, có khóa hàng món và ràng buộc database.
- Không xóa được loại đang có món; ID sai hiển thị trang lỗi thân thiện, không lộ SQL/stack trace.
- Lần đầu luôn English bất kể Accept-Language của trình duyệt. Chọn Tiếng Việt/English trên navbar; cookie ghi nhớ ngôn ngữ.
- Đổi ngôn ngữ trên List giữ query filter/sort. Label, nút, trạng thái, validation, thông báo nghiệp vụ/flash được dịch.
- Tên/mô tả món và loại là dữ liệu người dùng, không tự dịch.
- Bootstrap CSS lưu trong project; ảnh URL phụ thuộc máy chủ ảnh và mạng. Dữ liệu mẫu dùng ảnh minh họa chung; có thể thay bằng URL đúng từng món khi demo.

## Checklist video demo

1. Cho thấy Java 21, Gradle và package `ra.demo`; mở 3 Entity có hậu tố BIT240038.
2. Chạy app; lần đầu English. Mở danh sách có 3 loại và 5 món mẫu, gồm ngừng bán, Quick/Regular.
3. Tạo loại món; tạo món với loại vừa tạo; thử form rỗng, giá 0, thời gian 0.
4. Tạo món trùng tên trong cùng loại để thấy lỗi; cùng tên ở loại khác hợp lệ.
5. Sửa tên/giá/thời gian/trạng thái; mở Detail đối chiếu kết quả.
6. Kết hợp tên, category, availability, khoảng giá; thử từng sort; thử min > max và danh sách rỗng.
7. Giữ filter đang dùng, chuyển Tiếng Việt rồi English; điều hướng sang trang khác để thấy locale được nhớ.
8. Thêm hai URL ảnh; chọn/đổi thumbnail; thấy chỉ một ảnh đại diện. Thử URL sai.
9. Thử xóa loại đang có món; thấy thông báo. Xóa món demo rồi xóa loại demo rỗng.
10. Truy cập ID không tồn tại; thấy trang lỗi EN/VI.
11. Trong pgAdmin, chạy `sql/verify-schema.sql`: đúng database, đủ 14 cột, 2 FK, không cột thừa, tối đa một thumbnail.
12. Chạy lại app để thấy seed không nhân đôi; trình bày kết quả Gradle build/test.

## Kết quả xác minh
- `gradlew build`: **BUILD SUCCESSFUL** (Java 21).
- `gradlew test`: **18 passed / 20**, 2 test dành riêng PostgreSQL skipped theo điều kiện.
- `gradlew postgresTest`: **20/20 passed**, PostgreSQL **18.1 thật**, không dùng H2 cho lượt này.
- PostgreSQL kiểm thử tách biệt trong lượt đối chiếu PDF ngày 14/10/2026: `127.0.0.1:55439`, dữ liệu tại `build/postgres-review/data`. Chạy app trên `BIT240038_MID`; chạy test trên `BIT240038_MID_TEST`. Instance chính `localhost:5432` không bị sửa.
- Đã truy vấn `information_schema` trên `BIT240038_MID`: đúng 3 bảng, **14 cột**, **2 FK**, không cột thừa. Có unique index tên món trong loại và partial unique index thumbnail.
- Sau kiểm thử UI và dọn đúng bản ghi test tự tạo: **3 loại, 5 món, 5 ảnh**. Khởi động lại không nhân đôi dữ liệu.
- Playwright + Chromium: **13/13 luồng kiểm tra đạt**, gồm CRUD loại/món, form validation, Quick/Regular, món ngừng bán, trùng tên, filter kết hợp, locale/filter giữ nguyên, khoảng giá sai, danh sách rỗng, URL/multiple images/thumbnail, loại đang dùng, lỗi 404, xóa món/ảnh, EN↔VI, responsive.
- Đã xem screenshot desktop và mobile 390 px. Bảng trên mobile cuộn ngang trong vùng bảng; trang không tràn ngang.
- Browser test kiểm tra ảnh bằng fixture SVG để kết quả ổn định; không khẳng định mọi dịch vụ ảnh ngoài đang hoạt động.
- Screenshot: `build/browser-check/`; báo cáo Gradle: `build/reports/tests/`; SQL evidence: `build/schema-verification.txt`.
- Review bằng **ponytail:ponytail-review** theo luồng Controller → DTO/Validation → Service/transaction → Repository/SQL → view/message và các test, với tải demo thực hành. Không còn phát hiện cần sửa trong scope đã triển khai.
- Đã đọc đủ ba trang `ktgk.pdf` (trích xuất nội dung và xem ảnh từng trang); bảng đối chiếu bên trên sử dụng đúng tám tiêu chí, tổng trọng số 10,0 điểm.

| Tiêu chí chấm điểm theo đề gốc | Điểm tối đa | Đối chiếu source và kiểm thử thực tế |
|---|---:|---|
| Đặt tên database, ba bảng, tất cả cột SQL đúng tiền tố MSSV; tên Model đúng MSSV | 1,5 | Đạt trên PostgreSQL kiểm thử: database BIT240038_MID; ba bảng BIT240038_DishCategories, BIT240038_Dishes, BIT240038_DishImages; đúng 14 cột có tiền tố BIT240038_, không cột thừa; ba Model đúng MSSV. Chưa xác minh instance chính cổng 5432. |
| Thiết kế Model, validation và quan hệ dữ liệu | 1,5 | Đạt: quan hệ một loại–nhiều món, một món–nhiều ảnh, hai khóa ngoại; tên bắt buộc, Price > 0, PreparationTime > 0, loại món tồn tại, tên món không trùng trong cùng loại. Seed đủ ba loại/năm món, không nhân đôi khi chạy lại. |
| Create, Edit và dữ liệu liên kết | 1,0 | Đạt: Create/Edit món và loại món; dropdown chọn loại từ database; List/Detail/Delete hoạt động, xóa món kèm ảnh; dữ liệu nhập sai không được lưu. |
| Tìm kiếm, lọc, khoảng giá, sắp xếp và giữ điều kiện | 1,5 | Đạt: tên một phần, loại món, trạng thái, min/max price kết hợp; giá tăng/giảm, thời gian tăng; truy vấn bằng JPA Specification/Sort ở database; giữ giá trị sau submit và đổi ngôn ngữ, gồm ký tự tiếng Việt/ký tự đặc biệt. |
| Quản lý ảnh và ảnh đại diện | 1,0 | Đạt: thêm ImageUrl HTTP/HTTPS, hiển thị nhiều ảnh tại Detail, chọn/đổi thumbnail; khóa giao dịch và partial unique index bảo đảm tối đa một thumbnail/món; từ chối ảnh của món khác. |
| Phân loại thời gian, ràng buộc nghiệp vụ và xử lý lỗi | 1,0 | Đạt: Quick/Nhanh ở 15 phút, Regular/Thông thường ở 16 phút, tính khi hiển thị; chặn xóa loại đang dùng; món ngừng bán vẫn xuất hiện và bị loại khi lọc Available; xử lý ID thiếu của cả ba Model, khoảng giá sai, chưa có ảnh, không có kết quả, dữ liệu Create/Edit sai; lỗi thân thiện không lộ SQL/stack trace. |
| Đa ngôn ngữ: tiếng Anh mặc định, tiếng Việt và chuyển đổi nhất quán | 1,5 | Đạt: lần đầu English dù trình duyệt dùng tiếng Việt; nhãn/nút/bộ lọc/trạng thái/phân loại/validation/thông báo dịch EN/VI; chuyển qua lại trên List/Create/Edit/Detail; cookie nhớ ngôn ngữ khi điều hướng, giữ bộ lọc khi đổi ngôn ngữ. Tên/mô tả do người dùng nhập không cần dịch theo đề. |
| Giao diện rõ ràng, demo đầy đủ và nộp bài đúng yêu cầu | 1,0 | Giao diện desktop/mobile đã kiểm tra bằng Playwright và ảnh chụp. Chưa xác minh video demo, việc nộp source/video lên EDUX và thời hạn nộp. |
| **Tổng trọng số** | **10,0** | **Không khẳng định điểm chấm thực tế khi video và việc nộp bài chưa được xác minh.** |

Kiểm chứng bổ sung ngày 14/10/2026: **11/11 kiểm tra browser đạt**, gồm chuyển EN/VI trên bảy trang món/loại, giữ truy vấn có ký tự tiếng Việt và ký tự đặc biệt, ID thiếu của món/loại/ảnh. Gradle `test build --rerun-tasks` và `postgresTest --rerun-tasks` đều thành công; 18 test H2 đạt (hai test PostgreSQL bỏ qua có điều kiện), 20/20 test PostgreSQL đạt.

## Skill đã áp dụng
Đã đọc và áp dụng: `spring-boot-backend`, `postgresql-backend`,
`mattpocock-skills:domain-modeling` (GLOSSARY), `mattpocock-skills:codebase-design`,
`frontend-design`, `webapp-testing`, `ponytail:ponytail`,
`mattpocock-skills:diagnosing-bugs`, `ponytail:ponytail-review`.
Quy định Gradle của bài được ưu tiên hơn hướng dẫn Maven trong skill backend.

## Browser test tùy chọn
Chỉ chạy trên môi trường/database kiểm thử riêng. Script tạo bản ghi riêng có mã ngẫu nhiên và xóa đúng bản ghi vừa tạo sau khi thành công.

```powershell
python -m pip install playwright
python -m playwright install chromium
$env:DISH_BASE_URL = 'http://localhost:8080'
python scripts/browser_smoke.py
```

Có thể đặt `BROWSER_EXECUTABLE` vào đường dẫn Chromium có sẵn.
Nếu test dừng giữa chừng, các bản ghi có tên `Browser category <mã>` / `Browser dish <mã>` của lần chạy đó có thể còn trong database kiểm thử; kiểm tra và xóa qua giao diện khi cần.

## Nội dung chưa kiểm chứng
- Chưa kết nối/xác minh schema hoặc chạy ứng dụng trên instance PostgreSQL chính `localhost:5432`, vì không có `DB_PASSWORD`. Kết quả PostgreSQL thật trong lượt đối chiếu này thuộc instance riêng cổng 55439.
- Chưa thao tác trực tiếp IntelliJ; đã kiểm chứng bằng Gradle Wrapper và JAR Java 21.
- Chưa đảm bảo ảnh URL ngoài tồn tại ổn định; ảnh mẫu chỉ minh họa. Không tự dịch nội dung món/loại do người dùng nhập.


## Danh sách file đầy đủ

~~~text
.gitignore
build.gradle
settings.gradle
gradlew
gradlew.bat
GLOSSARY.md
README.md
gradle\wrapper\gradle-wrapper.jar
gradle\wrapper\gradle-wrapper.properties
scripts\browser_smoke.py
sql\create-database.sql
sql\verify-schema.sql
src\main\java\ra\demo\advice\GlobalExceptionHandler.java
src\main\java\ra\demo\config\SampleDataInitializer.java
src\main\java\ra\demo\config\WebConfig.java
src\main\java\ra\demo\controller\CategoryController.java
src\main\java\ra\demo\controller\DishController.java
src\main\java\ra\demo\DishManagementApplication.java
src\main\java\ra\demo\exception\BusinessException.java
src\main\java\ra\demo\exception\ResourceNotFoundException.java
src\main\java\ra\demo\model\dto\request\CategoryRequest.java
src\main\java\ra\demo\model\dto\request\DishFilter.java
src\main\java\ra\demo\model\dto\request\DishRequest.java
src\main\java\ra\demo\model\dto\request\ImageRequest.java
src\main\java\ra\demo\model\entity\Dish_BIT240038.java
src\main\java\ra\demo\model\entity\DishCategory_BIT240038.java
src\main\java\ra\demo\model\entity\DishImage_BIT240038.java
src\main\java\ra\demo\repository\DishCategoryRepository.java
src\main\java\ra\demo\repository\DishImageRepository.java
src\main\java\ra\demo\repository\DishRepository.java
src\main\java\ra\demo\repository\DishSpecifications.java
src\main\java\ra\demo\service\CategoryService.java
src\main\java\ra\demo\service\DishService.java
src\main\java\ra\demo\service\impl\CategoryServiceImpl.java
src\main\java\ra\demo\service\impl\DishServiceImpl.java
src\main\resources\application.properties
src\main\resources\messages.properties
src\main\resources\messages_vi.properties
src\main\resources\schema.sql
src\main\resources\static\css\app.css
src\main\resources\static\css\bootstrap.min.css
src\main\resources\static\js\app.js
src\main\resources\templates\categories\form.html
src\main\resources\templates\categories\list.html
src\main\resources\templates\dishes\detail.html
src\main\resources\templates\dishes\form.html
src\main\resources\templates\dishes\list.html
src\main\resources\templates\error.html
src\main\resources\templates\fragments\layout.html
src\test\java\ra\demo\DishManagementIntegrationTest.java
src\test\resources\schema-h2.sql
~~~
