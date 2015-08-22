package com.az.ip.api.services.model;

/**
 * Created by magnus on 21/08/15.
 */
public class Pageable {
    int page = 0;
    int size = 10;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
