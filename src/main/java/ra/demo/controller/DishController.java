package ra.demo.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ra.demo.exception.BusinessException;
import ra.demo.model.dto.request.*;
import ra.demo.service.*;
import java.util.List;
import java.util.Map;
@Controller @RequiredArgsConstructor
public class DishController {
    private final DishService dishes;
    private final CategoryService categories;
    @GetMapping("/")
    public String home() { return "redirect:/dishes"; }
    @GetMapping("/dishes")
    public String list(@Valid @ModelAttribute("filter") DishFilter filter, BindingResult result, Model model) {
        model.addAttribute("categories", categories.list());
        model.addAttribute("dishes", result.hasErrors() ? List.of() : dishes.search(filter));
        return "dishes/list";
    }
    @GetMapping("/dishes/new")
    public String create(Model model) {
        model.addAttribute("dishForm", new DishRequest());
        return form(null, model);
    }
    @GetMapping("/dishes/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("dishForm", dishes.getForm(id));
        return form(id, model);
    }
    @PostMapping("/dishes")
    public String create(@Valid @ModelAttribute("dishForm") DishRequest request, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        return save(null, request, result, model, redirect);
    }
    @PostMapping("/dishes/{id}")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("dishForm") DishRequest request,
                       BindingResult result, Model model, RedirectAttributes redirect) {
        dishes.get(id);
        return save(id, request, result, model, redirect);
    }
    @GetMapping("/dishes/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("imageForm", new ImageRequest());
        return detailModel(id, model);
    }
    @PostMapping("/dishes/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        dishes.delete(id);
        redirect.addFlashAttribute("successKey", "flash.dish.deleted");
        return "redirect:/dishes";
    }
    @PostMapping("/dishes/{id}/images")
    public String addImage(@PathVariable Long id, @Valid @ModelAttribute("imageForm") ImageRequest request,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) return detailModel(id, model);
        dishes.addImage(id, request);
        redirect.addFlashAttribute("successKey", "flash.image.added");
        return "redirect:/dishes/" + id;
    }
    @PostMapping("/dishes/{id}/images/{imageId}/thumbnail")
    public String thumbnail(@PathVariable Long id, @PathVariable Long imageId, RedirectAttributes redirect) {
        dishes.selectThumbnail(id, imageId);
        redirect.addFlashAttribute("successKey", "flash.thumbnail.changed");
        return "redirect:/dishes/" + id;
    }
    private String save(Long id, DishRequest request, BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                Long savedId = dishes.save(id, request);
                redirect.addFlashAttribute("successKey", id == null ? "flash.dish.created" : "flash.dish.updated");
                return "redirect:/dishes/" + savedId;
            } catch (BusinessException ex) {
                result.rejectValue(ex.getField(), ex.getMessageKey());
            }
        }
        return form(id, model);
    }
    private String form(Long id, Model model) {
        String path = id == null ? "/dishes/new" : "/dishes/" + id + "/edit";
        model.addAttribute("languageLinks", Map.of("en", path + "?lang=en", "vi", path + "?lang=vi"));
        model.addAttribute("dishId", id);
        model.addAttribute("categories", categories.list());
        return "dishes/form";
    }
    private String detailModel(Long id, Model model) {
        String path = "/dishes/" + id;
        model.addAttribute("languageLinks", Map.of("en", path + "?lang=en", "vi", path + "?lang=vi"));
        model.addAttribute("dish", dishes.get(id));
        return "dishes/detail";
    }
}
