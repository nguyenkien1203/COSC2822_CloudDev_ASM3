package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.MenuItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MenuServiceClientFallback implements MenuServiceClient {
    @Override
    public MenuItemDto getMenuItemById(String id) {
        log.warn("Fallback: Menu service unavailable, returning default for item: {}", id);
        return MenuItemDto.builder()
                .id(id)
                .name("Menu Item #" + id + " (Unavailable)")
                .price(BigDecimal.ZERO)
                .isAvailable(false)
                .build();
    }

    @Override
    public List<MenuItemDto> getAvailableMenuItems() {
        log.warn("Fallback: Menu service unavailable for available items");
        return Collections.emptyList();
    }
}
