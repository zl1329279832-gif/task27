package com.training.dto.response;

import com.github.pagehelper.Page;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> records;

    private long total;

    private int page;

    private int size;

    private int pages;

    public static <T> PageResult<T> of(List<T> list) {
        PageResult<T> result = new PageResult<>();
        if (list instanceof Page) {
            Page<T> pageInfo = (Page<T>) list;
            result.setRecords(pageInfo.getResult());
            result.setTotal(pageInfo.getTotal());
            result.setPage(pageInfo.getPageNum());
            result.setSize(pageInfo.getPageSize());
            result.setPages(pageInfo.getPages());
        } else {
            result.setRecords(list);
            result.setTotal(list.size());
            result.setPage(1);
            result.setSize(list.size());
            result.setPages(1);
        }
        return result;
    }
}
