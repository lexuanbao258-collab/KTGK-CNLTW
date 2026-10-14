"""Browser checks against a running app and a separate test database.
Run with DISH_BASE_URL pointing to the test server, never to a shared database.
"""
import os
import re
import uuid
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from playwright.sync_api import sync_playwright, expect

base = os.environ.get("DISH_BASE_URL", "http://localhost:8080")
executable = os.environ.get("BROWSER_EXECUTABLE")
output = Path("build/browser-check")
output.mkdir(parents=True, exist_ok=True)
tag = uuid.uuid4().hex[:8]
category_name = f"Browser category {tag}"
dish_name = f"Browser dish {tag}"
checks = []
def check(name):
    checks.append(name)
    print(f"PASS: {name}", flush=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True, executable_path=executable)
    context = browser.new_context(locale="vi-VN", viewport={"width": 1440, "height": 1000})
    page = context.new_page()
    failures = []
    page.on("pageerror", lambda error: failures.append(str(error)))
    page.on("response", lambda response: failures.append(f"HTTP {response.status} {response.url}")
            if response.url.startswith(base) and response.status >= 500 else None)
    # Deterministic browser fixtures; image URL persistence is still exercised.
    context.route("https://example.com/browser-*.svg", lambda route: route.fulfill(
        content_type="image/svg+xml",
        body='<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500"><rect width="800" height="500" fill="#dfe9eb"/><circle cx="400" cy="250" r="140" fill="#efbc52"/><text x="400" y="270" text-anchor="middle" font-size="44" fill="#173c47">Demo image</text></svg>'))
    page.goto(base + "/dishes")
    page.wait_for_load_state("networkidle")
    expect(page.get_by_role("heading", name="Kitchen dishes", exact=True)).to_be_visible()
    assert page.locator("html").get_attribute("lang") == "en"
    page.screenshot(path=str(output / "list-en.png"), full_page=True)
    check("English default despite Vietnamese browser locale")

    page.get_by_role("link", name="Categories", exact=True).click()
    page.get_by_role("link", name="Add category", exact=True).click()
    page.get_by_label("Category name", exact=True).fill(category_name)
    page.get_by_label("Description", exact=True).fill("Browser test category")
    page.get_by_role("button", name="Save", exact=True).click()
    row = page.locator("tbody tr").filter(has_text=category_name)
    expect(row).to_have_count(1)
    row.get_by_role("link", name="Edit", exact=True).click()
    category_name += " edited"
    page.get_by_label("Category name", exact=True).fill(category_name)
    category_id = re.search(r"/categories/(\d+)/edit", page.url).group(1)
    page.get_by_role("button", name="Save", exact=True).click()
    expect(page.locator("tbody tr").filter(has_text=category_name)).to_have_count(1)
    check("Category create and edit")

    page.get_by_role("link", name="Dishes", exact=True).click()
    page.get_by_role("link", name="Add dish", exact=True).click()
    page.get_by_role("button", name="Save", exact=True).click()
    expect(page.get_by_text("Name is required.", exact=True)).to_be_visible()
    expect(page.get_by_text("Price is required.", exact=True)).to_be_visible()
    page.get_by_role("link", name="Tiếng Việt", exact=True).click()
    expect(page.get_by_role("heading", name="Thêm món ăn", exact=True)).to_be_visible()
    page.get_by_role("link", name="English", exact=True).click()
    expect(page.get_by_role("heading", name="Add dish", exact=True)).to_be_visible()
    page.get_by_label("Dish name", exact=True).fill(dish_name)
    page.get_by_label("Price (VND)", exact=True).fill("50000")
    page.get_by_label("Preparation time (minutes)", exact=True).fill("15")
    page.get_by_label("Category", exact=True).select_option(category_id)
    page.get_by_label("Available", exact=True).uncheck()
    page.get_by_role("button", name="Save", exact=True).click()
    expect(page.get_by_text("Dish created.", exact=True)).to_be_visible()
    expect(page.get_by_text("Quick", exact=True)).to_be_visible()
    expect(page.get_by_text("Unavailable", exact=True)).to_be_visible()
    expect(page.get_by_text("This dish has no images. Add an image URL below.", exact=True)).to_be_visible()
    detail_url = page.url
    dish_id = detail_url.rsplit("/", 1)[1]
    check("Dish create, validation, unavailable state and Quick boundary")

    page.get_by_role("link", name="Edit", exact=True).click()
    page.get_by_label("Preparation time (minutes)", exact=True).fill("16")
    page.get_by_label("Price (VND)", exact=True).fill("60000")
    page.get_by_role("button", name="Save", exact=True).click()
    expect(page.get_by_text("Regular", exact=True)).to_be_visible()
    check("Dish edit and Regular boundary")

    page.goto(base + "/dishes/new")
    page.get_by_label("Dish name", exact=True).fill(dish_name.upper())
    page.get_by_label("Price (VND)", exact=True).fill("65000")
    page.get_by_label("Preparation time (minutes)", exact=True).fill("10")
    page.get_by_label("Category", exact=True).select_option(category_id)
    page.get_by_role("button", name="Save", exact=True).click()
    expect(page.get_by_text("A dish with this name already exists in this category.", exact=True)).to_be_visible()
    check("Duplicate dish within category rejected")

    page.goto(base + "/dishes")
    page.get_by_label("Search dishes", exact=True).fill(tag)
    page.get_by_label("Category", exact=True).select_option(category_id)
    page.get_by_label("Availability", exact=True).select_option("false")
    page.get_by_label("Minimum price", exact=True).fill("55000")
    page.get_by_label("Maximum price", exact=True).fill("65000")
    page.get_by_label("Sort by", exact=True).select_option("priceDesc")
    page.get_by_role("button", name="Apply filters", exact=True).click()
    expect(page.locator("tbody tr")).to_have_count(1)
    expect(page.locator("tbody tr")).to_contain_text(dish_name)
    original_query = parse_qs(urlparse(page.url).query)
    page.get_by_role("link", name="Tiếng Việt", exact=True).click()
    expect(page.get_by_role("heading", name="Danh sách món ăn", exact=True)).to_be_visible()
    switched_query = parse_qs(urlparse(page.url).query)
    for key in ("name", "categoryId", "available", "minPrice", "maxPrice", "sort"):
        assert switched_query[key] == original_query[key], key
    expect(page.locator("#name")).to_have_value(tag)
    expect(page.locator("#available")).to_have_value("false")
    expect(page.locator("#sort")).to_have_value("priceDesc")
    page.screenshot(path=str(output / "list-filtered-vi.png"), full_page=True)
    check("Combined database filters and language switch preserve all filter/sort values")

    page.get_by_label("Giá tối thiểu", exact=True).fill("70000")
    page.get_by_role("button", name="Lọc món ăn", exact=True).click()
    expect(page.get_by_text("Giá tối thiểu không được lớn hơn giá tối đa.", exact=True)).to_be_visible()
    page.get_by_role("link", name="Đặt lại", exact=True).click()
    expect(page.get_by_role("heading", name="Danh sách món ăn", exact=True)).to_be_visible()
    page.get_by_label("Tìm món ăn", exact=True).fill("no-result-" + tag)
    page.get_by_role("button", name="Lọc món ăn", exact=True).click()
    expect(page.get_by_text("Không có món phù hợp. Hãy đổi bộ lọc hoặc thêm món ăn.", exact=True)).to_be_visible()
    check("Vietnamese range error, empty results and persistent locale")

    page.goto(detail_url)
    expect(page.get_by_text("Thông thường", exact=True)).to_be_visible()
    expect(page.get_by_text("Ngừng bán", exact=True)).to_be_visible()
    page.get_by_label("Đường dẫn ảnh (HTTP hoặc HTTPS)", exact=True).fill("javascript:alert(1)")
    page.get_by_role("button", name="Thêm ảnh", exact=True).click()
    expect(page.get_by_text("Hãy nhập đường dẫn ảnh HTTP hoặc HTTPS hợp lệ.", exact=True)).to_be_visible()
    page.get_by_role("link", name="English", exact=True).click()
    expect(page.get_by_role("heading", name=dish_name, exact=True)).to_be_visible()
    assert page.url.endswith(f"/dishes/{dish_id}?lang=en")
    page.get_by_role("link", name="Tiếng Việt", exact=True).click()
    for n in (1, 2):
        page.get_by_label("Đường dẫn ảnh (HTTP hoặc HTTPS)", exact=True).fill(f"https://example.com/browser-{n}.svg")
        page.get_by_label("Dùng ảnh này làm ảnh đại diện", exact=True).check()
        page.get_by_role("button", name="Thêm ảnh", exact=True).click()
        expect(page.get_by_text("Đã thêm ảnh.", exact=True)).to_be_visible()
    expect(page.locator(".image-card")).to_have_count(2)
    expect(page.get_by_text("Ảnh đại diện", exact=True)).to_have_count(1)
    page.locator(".image-card").first.get_by_role("button", name="Chọn làm ảnh đại diện", exact=True).click()
    expect(page.get_by_text("Đã đổi ảnh đại diện.", exact=True)).to_be_visible()
    expect(page.locator(".image-card").first.get_by_text("Ảnh đại diện", exact=True)).to_be_visible()
    expect(page.get_by_text("Ảnh đại diện", exact=True)).to_have_count(1)
    page.wait_for_load_state("networkidle")
    page.screenshot(path=str(output / "detail-vi.png"), full_page=True)
    check("Multiple images, URL validation and thumbnail replacement")

    page.get_by_role("link", name="Loại món", exact=True).click()
    row = page.locator("tbody tr").filter(has_text=category_name)
    page.once("dialog", lambda dialog: dialog.accept())
    row.get_by_role("button", name="Xóa", exact=True).click()
    expect(page.get_by_text("Không thể xóa loại món đang có món ăn.", exact=True)).to_be_visible()
    check("Used category cannot be deleted")

    response = page.goto(base + "/dishes/9223372036854775807")
    assert response.status == 404
    expect(page.get_by_text("Món ăn không tồn tại.", exact=True)).to_be_visible()
    assert "org.hibernate" not in page.content() and "Exception at" not in page.content()
    check("Friendly translated 404")

    page.goto(base + "/dishes")
    page.set_viewport_size({"width": 390, "height": 844})
    page.wait_for_load_state("networkidle")
    assert page.evaluate("document.documentElement.scrollWidth <= window.innerWidth")
    expect(page.get_by_role("link", name="Tiếng Việt", exact=True)).to_be_visible()
    page.screenshot(path=str(output / "list-mobile-vi.png"), full_page=True)
    check("Mobile layout without page overflow")

    # Delete only the two records this script created.
    page.set_viewport_size({"width": 1440, "height": 1000})
    row = page.locator("tbody tr").filter(has_text=dish_name)
    page.once("dialog", lambda dialog: dialog.accept())
    row.get_by_role("button", name="Xóa", exact=True).click()
    expect(page.get_by_text("Đã xóa món ăn và các ảnh.", exact=True)).to_be_visible()
    page.get_by_role("link", name="Loại món", exact=True).click()
    row = page.locator("tbody tr").filter(has_text=category_name)
    page.once("dialog", lambda dialog: dialog.accept())
    row.get_by_role("button", name="Xóa", exact=True).click()
    expect(page.get_by_text("Đã xóa loại món.", exact=True)).to_be_visible()
    check("Delete own test dish with images, then delete empty category")

    page.get_by_role("link", name="English", exact=True).click()
    expect(page.get_by_role("heading", name="Dish categories", exact=True)).to_be_visible()
    page.get_by_role("link", name="Dishes", exact=True).click()
    expect(page.get_by_role("heading", name="Kitchen dishes", exact=True)).to_be_visible()
    assert not failures, failures
    check("Switch back to English, no JavaScript errors or HTTP 500")
    browser.close()
print(f"BROWSER_CHECKS={len(checks)} ALL_PASSED", flush=True)
