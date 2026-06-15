package com.personal.jz.module.stats;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.module.book.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "统计", description = "统计相关接口")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final BookService bookService;

    @GetMapping("/monthly")
    public ApiResponse<Map<String, Object>> monthly(@CurrentUser Long uid, @RequestParam Long bookId, @RequestParam String yearMonth) {
        bookService.get(uid, bookId);
        return ApiResponse.ok(statsService.monthly(bookId, yearMonth));
    }

    @GetMapping("/category-breakdown")
    public ApiResponse<List<Map<String, Object>>> breakdown(@CurrentUser Long uid, @RequestParam Long bookId,
                                                             @RequestParam String yearMonth, @RequestParam(required = false) String type) {
        bookService.get(uid, bookId);
        return ApiResponse.ok(statsService.breakdown(bookId, yearMonth, type));
    }

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(@CurrentUser Long uid, @RequestParam Long bookId,
                                                         @RequestParam(defaultValue = "6") int rangeMonths) {
        bookService.get(uid, bookId);
        return ApiResponse.ok(statsService.trend(bookId, rangeMonths));
    }
}
