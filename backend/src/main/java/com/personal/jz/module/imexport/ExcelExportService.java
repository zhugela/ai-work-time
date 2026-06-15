package com.personal.jz.module.imexport;

import com.personal.jz.entity.TxRecord;
import com.personal.jz.repository.TxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final TxRecordRepository txRepo;

    public byte[] exportTransactions(Long bookId, LocalDate from, LocalDate to) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("收支明细");
            Row h = sh.createRow(0);
            String[] cols = {"日期", "类型", "分类", "金额", "支付方式", "备注"};
            for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
            List<TxRecord> rows = txRepo.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TxRecord>()
                    .eq(TxRecord::getBookId, bookId)
                    .eq(TxRecord::getIsDeleted, 0)
                    .ge(TxRecord::getOccurredAt, from.atStartOfDay())
                    .le(TxRecord::getOccurredAt, to.atTime(23, 59, 59))
                    .orderByDesc(TxRecord::getOccurredAt));
            int r = 1;
            for (TxRecord t : rows) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(t.getOccurredAt().toString());
                row.createCell(1).setCellValue(t.getType());
                row.createCell(2).setCellValue(String.valueOf(t.getCategoryId()));
                row.createCell(3).setCellValue(t.getAmount().toPlainString());
                row.createCell(4).setCellValue(t.getPayMethod());
                row.createCell(5).setCellValue(t.getRemark() == null ? "" : t.getRemark());
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("导入模板");
            Row h = sh.createRow(0);
            String[] cols = {"日期(YYYY-MM-DD)", "金额", "类型(INCOME/EXPENSE)", "分类名", "支付方式", "备注"};
            for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
            Row e = sh.createRow(1);
            e.createCell(0).setCellValue(LocalDate.now().toString());
            e.createCell(1).setCellValue("38.00");
            e.createCell(2).setCellValue("EXPENSE");
            e.createCell(3).setCellValue("餐饮");
            e.createCell(4).setCellValue("ALIPAY");
            e.createCell(5).setCellValue("示例");
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }
}
