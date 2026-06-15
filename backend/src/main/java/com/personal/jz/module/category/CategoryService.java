package com.personal.jz.module.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.Category;
import com.personal.jz.entity.TxRecord;
import com.personal.jz.repository.CategoryRepository;
import com.personal.jz.repository.TxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final TxRecordRepository txRepo;
    private static final int MAX_PER_BOOK = 100;

    public List<Category> list(Long bookId, String type) {
        var q = new LambdaQueryWrapper<Category>().eq(Category::getBookId, bookId);
        if (type != null && !type.isEmpty()) q.eq(Category::getType, type);
        q.orderByAsc(Category::getSortOrder).orderByAsc(Category::getId);
        return categoryRepo.selectList(q);
    }

    @Transactional
    public Category create(Long bookId, Category in) {
        long count = categoryRepo.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getBookId, bookId));
        if (count >= MAX_PER_BOOK) throw new BizException(ErrorCodeEnums.CATEGORY_LIMIT_REACHED);
        Long dup = categoryRepo.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getBookId, bookId).eq(Category::getName, in.getName()).eq(Category::getType, in.getType()));
        if (dup != null && dup > 0) throw new BizException(ErrorCodeEnums.CATEGORY_NAME_TAKEN);
        Category c = new Category();
        c.setBookId(bookId);
        c.setName(in.getName());
        c.setType(in.getType());
        c.setIcon(in.getIcon());
        c.setColor(in.getColor());
        c.setIsSystem(0);
        c.setSortOrder(in.getSortOrder() == null ? 0 : in.getSortOrder());
        categoryRepo.insert(c);
        return c;
    }

    @Transactional
    public Category update(Long bookId, Long id, Category in) {
        Category c = categoryRepo.selectById(id);
        if (c == null || !c.getBookId().equals(bookId)) throw new BizException(ErrorCodeEnums.NOT_FOUND);
        if (c.getIsSystem() != null && c.getIsSystem() == 1) throw new BizException(ErrorCodeEnums.SYSTEM_CATEGORY_READONLY);
        if (in.getName() != null) c.setName(in.getName());
        if (in.getIcon() != null) c.setIcon(in.getIcon());
        if (in.getColor() != null) c.setColor(in.getColor());
        if (in.getSortOrder() != null) c.setSortOrder(in.getSortOrder());
        categoryRepo.updateById(c);
        return c;
    }

    @Transactional
    public void delete(Long bookId, Long id) {
        Category c = categoryRepo.selectById(id);
        if (c == null || !c.getBookId().equals(bookId)) throw new BizException(ErrorCodeEnums.NOT_FOUND);
        if (c.getIsSystem() != null && c.getIsSystem() == 1) throw new BizException(ErrorCodeEnums.SYSTEM_CATEGORY_READONLY);
        long used = txRepo.selectCount(new LambdaQueryWrapper<TxRecord>().eq(TxRecord::getCategoryId, id));
        if (used > 0) throw new BizException(ErrorCodeEnums.CATEGORY_IN_USE);
        categoryRepo.deleteById(id);
    }
}
