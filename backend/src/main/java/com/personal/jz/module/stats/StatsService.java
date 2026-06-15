package com.personal.jz.module.stats;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.personal.jz.entity.TxRecord;
import com.personal.jz.repository.TxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TxRecordRepository txRepo;

    public Map<String, Object> monthly(Long bookId, String ym) {
        LocalDate from = LocalDate.parse(ym + "-01");
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        QueryWrapper<TxRecord> qw = new QueryWrapper<>();
        qw.select("type, COALESCE(SUM(amount), 0) AS total")
          .eq("book_id", bookId)
          .eq("is_deleted", 0)
          .ge("occurred_at", from.atStartOfDay())
          .le("occurred_at", to.atTime(23, 59, 59))
          .groupBy("type");
        List<Map<String, Object>> rows = txRepo.selectMaps(qw);
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (var r : rows) {
            String t = String.valueOf(r.get("type"));
            BigDecimal v = new BigDecimal(String.valueOf(r.get("total")));
            if ("INCOME".equals(t)) income = v;
            else if ("EXPENSE".equals(t)) expense = v;
        }
        BigDecimal balance = income.subtract(expense);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("yearMonth", ym);
        r.put("income", income);
        r.put("expense", expense);
        r.put("balance", balance);
        r.put("savingsRate", income.signum() == 0 ? 0.0
                : balance.multiply(BigDecimal.valueOf(100)).divide(income, 1, java.math.RoundingMode.HALF_UP).doubleValue());
        return r;
    }

    public List<Map<String, Object>> breakdown(Long bookId, String ym, String type) {
        String t = (type == null || type.isEmpty()) ? "EXPENSE" : type;
        LocalDate from = LocalDate.parse(ym + "-01");
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        QueryWrapper<TxRecord> qw = new QueryWrapper<>();
        qw.select("category_id, COALESCE(SUM(amount), 0) AS total")
          .eq("book_id", bookId)
          .eq("type", t)
          .eq("is_deleted", 0)
          .ge("occurred_at", from.atStartOfDay())
          .le("occurred_at", to.atTime(23, 59, 59))
          .groupBy("category_id")
          .orderByDesc("total");
        List<Map<String, Object>> rows = txRepo.selectMaps(qw);
        BigDecimal sum = BigDecimal.ZERO;
        for (var r : rows) sum = sum.add(new BigDecimal(String.valueOf(r.get("total"))));
        List<Map<String, Object>> out = new ArrayList<>();
        if (sum.signum() == 0) return out;
        for (var r : rows) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("categoryId", r.get("category_id"));
            e.put("amount", r.get("total"));
            BigDecimal v = new BigDecimal(String.valueOf(r.get("total")));
            double pct = v.multiply(BigDecimal.valueOf(100)).divide(sum, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            e.put("percentage", pct);
            out.add(e);
        }
        return out;
    }

    public List<Map<String, Object>> trend(Long bookId, int months) {
        if (months != 6 && months != 12) months = 6;
        LocalDate end = LocalDate.now();
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate m = end.minusMonths(i).withDayOfMonth(1);
            String ym = String.format("%04d-%02d", m.getYear(), m.getMonthValue());
            Map<String, Object> row = monthly(bookId, ym);
            row.remove("savingsRate");
            out.add(row);
        }
        return out;
    }
}
