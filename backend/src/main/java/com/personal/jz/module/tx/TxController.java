package com.personal.jz.module.tx;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.api.PageResponse;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.entity.TxRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "收支", description = "收支相关接口")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TxController {

    private final TxService txService;

    @PostMapping
    public ApiResponse<TxRecord> create(@CurrentUser Long uid, @RequestBody TxRecord in) {
        return ApiResponse.ok(txService.create(uid, in));
    }

    @GetMapping
    public ApiResponse<PageResponse<TxRecord>> list(@CurrentUser Long uid,
            @RequestParam Long bookId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(txService.list(uid, bookId, from, to, categoryId, type, q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TxRecord> get(@CurrentUser Long uid, @PathVariable Long id) {
        return ApiResponse.ok(txService.get(uid, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TxRecord> update(@CurrentUser Long uid, @PathVariable Long id, @RequestBody TxRecord in) {
        return ApiResponse.ok(txService.update(uid, id, in));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser Long uid, @PathVariable Long id) {
        txService.softDelete(uid, id);
        return ApiResponse.ok();
    }
}
