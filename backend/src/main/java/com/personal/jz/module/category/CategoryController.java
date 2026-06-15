package com.personal.jz.module.category;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.entity.Book;
import com.personal.jz.entity.Category;
import com.personal.jz.module.book.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "分类", description = "分类相关接口")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final BookService bookService;

    @GetMapping
    public ApiResponse<List<Category>> list(@CurrentUser Long uid, @RequestParam Long bookId, @RequestParam(required = false) String type) {
        bookService.get(uid, bookId); // 归属校验
        return ApiResponse.ok(categoryService.list(bookId, type));
    }

    @PostMapping
    public ApiResponse<Category> create(@CurrentUser Long uid, @RequestBody Category in) {
        if (in.getBookId() == null) throw new BizException(ErrorCodeEnums.PARAM_INVALID);
        bookService.get(uid, in.getBookId());
        return ApiResponse.ok(categoryService.create(in.getBookId(), in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Category> update(@CurrentUser Long uid, @PathVariable Long id, @RequestBody Category in) {
        Category c = categoryService.list(in.getBookId(), null).stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (c == null) throw new BizException(ErrorCodeEnums.NOT_FOUND);
        return ApiResponse.ok(categoryService.update(in.getBookId(), id, in));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser Long uid, @PathVariable Long id, @RequestParam Long bookId) {
        categoryService.delete(bookId, id);
        return ApiResponse.ok();
    }
}
