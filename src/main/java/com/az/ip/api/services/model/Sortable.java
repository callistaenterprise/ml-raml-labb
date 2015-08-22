package com.az.ip.api.services.model;

/**
 * Created by magnus on 21/08/15.
 */
public class Sortable {
    public enum Order {asc, desc;}

    private String orderBy = null;
    private Order order = Order.asc;

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
