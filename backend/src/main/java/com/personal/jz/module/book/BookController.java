package com.personal.jz.module.book;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ApiResponse<List<Book>> list(@CurrentUser Long uid, @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ApiResponse.ok(bookService.list(uid, includeArchived));
    }

    @GetMapping("/{id}")
    public ApiResponse<Book> get(@CurrentUser Long uid, @PathVariable Long id) {
        return ApiResponse.ok(bookService.get(uid, id));
    }

    @PostMapping
    public ApiResponse<Book> create(@CurrentUser Long uid, @RequestBody Book in) {
        return ApiResponse.ok(bookService.create(uid, in));
    }

    @PostMapping("/{id}/switch")
    public ApiResponse<Void> switchBook(@CurrentUser Long uid, @PathVariable Long id) {
        bookService.switchBook(uid, id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Book> updateStatus(@CurrentUser Long uid, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(bookService.updateStatus(uid, id, body.get("status")));
    }
}
