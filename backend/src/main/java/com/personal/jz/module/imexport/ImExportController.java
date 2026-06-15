package com.personal.jz.module.imexport;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.module.book.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ImExportController {

    private final ExcelExportService exportService;
    private final ExcelImportService importService;
    private final BookService bookService;

    @GetMapping("/api/exports/template")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] data = exportService.exportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/api/exports/transactions")
    public ResponseEntity<byte[]> transactions(@CurrentUser Long uid,
                                                @RequestParam Long bookId,
                                                @RequestParam LocalDate from,
                                                @RequestParam LocalDate to) throws IOException {
        bookService.get(uid, bookId);
        byte[] data = exportService.exportTransactions(bookId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=transactions-" + from + "-" + to + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/api/imports/transactions")
    public ApiResponse<Map<String, Object>> importTx(@CurrentUser Long uid,
                                                     @RequestParam Long bookId,
                                                     @RequestParam MultipartFile file) throws IOException {
        bookService.get(uid, bookId);
        if (file.isEmpty()) throw new BizException(ErrorCodeEnums.PARAM_INVALID);
        if (file.getSize() > 5 * 1024 * 1024) throw new BizException(ErrorCodeEnums.FILE_TOO_LARGE);
        Map<String, Object> r = importService.importTx(uid, bookId, file);
        return ApiResponse.ok(r);
    }
}
