package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.MenuItemDto;
import com.restaurant.orderservice.config.FeignClientConfig; // Import the config
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Add configuration = FeignClientConfig.class to apply the interceptor
// Uses url from application.yml: menu-service.url
@FeignClient(name = "menu-service", url = "${menu-service.url:}", configuration = FeignClientConfig.class, fallback = MenuServiceClientFallback.class)
public interface MenuServiceClient {

    @GetMapping("/api/menu/{id}")
    MenuItemDto getMenuItemById(@PathVariable("id") String id);

    // // Changed to use RequestParam (standard REST)
    // // URL will look like: /api/menu/batch?ids=1,2,3
    // @GetMapping("/api/menu/batch")
    // List<MenuItemDto> getMenuItemsByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/api/menu/available")
    List<MenuItemDto> getAvailableMenuItems();
}