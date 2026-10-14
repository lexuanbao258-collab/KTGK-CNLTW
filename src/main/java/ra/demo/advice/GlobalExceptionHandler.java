package ra.demo.advice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.UriComponentsBuilder;
import ra.demo.exception.*;
import java.util.Map;
@ControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    @ModelAttribute("languageLinks")
    public Map<String, String> languageLinks(HttpServletRequest request) {
        var builder = UriComponentsBuilder.fromPath(request.getRequestURI()).query(request.getQueryString());
        return Map.of("en", builder.cloneBuilder().replaceQueryParam("lang", "en").build(true).toUriString(),
                      "vi", builder.cloneBuilder().replaceQueryParam("lang", "vi").build(true).toUriString());
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorKey", ex.getMessageKey());
        return "error";
    }
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String business(BusinessException ex, Model model) {
        model.addAttribute("errorKey", ex.getMessageKey());
        return "error";
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String integrity(Model model) {
        model.addAttribute("errorKey", "error.conflict");
        return "error";
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidId(Model model) {
        model.addAttribute("errorKey", "error.input");
        return "error";
    }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missingPage(Model model) {
        model.addAttribute("errorKey", "error.page.notfound");
        return "error";
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, Model model) {
        log.error("Unhandled dish management error", ex);
        model.addAttribute("errorKey", "error.unexpected");
        return "error";
    }
}
