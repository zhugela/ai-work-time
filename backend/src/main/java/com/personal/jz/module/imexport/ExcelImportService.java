package com.personal.jz.module.imexport;

import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.Category;
import com.personal.jz.entity.TxRecord;
import com.personal.jz.repository.CategoryRepository;
import com.personal.jz.repository.TxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final CategoryRepository categoryRepo;
    private final TxRecordRepository txRepo;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_ROWS = 5000;

    @Transactional
    public Map<String, Object> importTx(Long uid, Long bookId, MultipartFile file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        int success = 0;

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sh = wb.getSheetAt(0);
            if (sh.getPhysicalNumberOfRows() > MAX_ROWS) throw new BizException(ErrorCodeEnums.ROW_LIMIT);

            // 表头校验
            Row header = sh.getRow(0);
            if (header == null || header.getPhysicalNumberOfCells() < 6) throw new BizException(ErrorCodeEnums.INVALID_HEADER);
            if (!"日期".equals(cellString(header.getCell(0)))) throw new BizException(ErrorCodeEnums.INVALID_HEADER);

            // 缓存账本分类（按名查）
            Map<String, Category> cache = new HashMap<>();
            for (int i = 1; i <= sh.getLastRowNum(); i++) {
                Row r = sh.getRow(i);
                if (r == null) continue;
                try {
                    // 公式注入拦截
                    for (int c = 0; c < 6; c++) {
                        Cell cell = r.getCell(c);
                        if (cell != null && cell.getCellType() == CellType.FORMULA) {
                            throw new BizException(ErrorCodeEnums.FORMULA_INJECTION);
                        }
                    }
                    String dateStr = cellString(r.getCell(0));
                    String amtStr = cellString(r.getCell(1));
                    String typeStr = cellString(r.getCell(2));
                    String catName = cellString(r.getCell(3));
                    String payStr = cellString(r.getCell(4));
                    String remark = cellString(r.getCell(5));

                    if (dateStr.isEmpty()) throw new BizException(ErrorCodeEnums.PARAM_INVALID);
                    LocalDate d = LocalDate.parse(dateStr, FMT);
                    BigDecimal amt = new BigDecimal(amtStr);
                    if (amt.signum() <= 0) throw new BizException(ErrorCodeEnums.INVALID_AMOUNT);
                    if (!"INCOME".equals(typeStr) && !"EXPENSE".equals(typeStr)) throw new BizException(ErrorCodeEnums.PARAM_INVALID);

                    String key = typeStr + ":" + catName;
                    Category c = cache.get(key);
                    if (c == null) {
                        c = categoryRepo.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                                        .eq(Category::getBookId, bookId)
                                        .eq(Category::getName, catName)
                                        .eq(Category::getType, typeStr));
                        if (c == null) throw new BizException(ErrorCodeEnums.CATEGORY_NOT_FOUND);
                        cache.put(key, c);
                    }

                    TxRecord t = new TxRecord();
                    t.setBookId(bookId); t.setUserId(uid);
                    t.setCategoryId(c.getId()); t.setType(typeStr);
                    t.setAmount(amt); t.setPayMethod(payStr.isEmpty() ? "CASH" : payStr);
                    t.setOccurredAt(d.atStartOfDay()); t.setRemark(remark);
                    txRepo.insert(t);
                    success++;
                } catch (Exception e) {
                    Map<String, Object> fail = new HashMap<>();
                    fail.put("row", i + 1);
                    fail.put("msg", e.getMessage());
                    failed.add(fail);
                }
            }
        }
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }

    private String cellString(Cell c) {
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> {
                double v = c.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) yield String.valueOf((long) v);
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }
}
