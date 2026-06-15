package com.personal.jz.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> list;
    private long page;
    private long size;
    private long total;
    private boolean hasNext;

    public static <T> PageResponse<T> of(List<T> list, long page, long size, long total) {
        return new PageResponse<>(list, page, size, total, page * size < total);
    }
}
