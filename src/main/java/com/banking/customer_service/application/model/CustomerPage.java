package com.banking.customer_service.application.model;

import java.util.List;

/**
 * One page of customers. Items and total are read separately, so concurrent
 * writes may make them disagree; the contract documents that pages are not snapshots.
 */
public record CustomerPage(
        List<CustomerView> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {
    public CustomerPage {
        items = List.copyOf(items);
    }

    public static CustomerPage of(List<CustomerView> items, int page, int size, long totalElements) {
        long totalPages = totalElements == 0 ? 0 : (totalElements + size - 1) / size;
        return new CustomerPage(items, page, size, totalElements, totalPages);
    }
}
