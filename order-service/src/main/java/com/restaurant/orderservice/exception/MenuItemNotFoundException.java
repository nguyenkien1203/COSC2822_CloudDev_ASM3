// MenuItemNotFoundException.java
package com.restaurant.orderservice.exception;

public class MenuItemNotFoundException extends RuntimeException {
    public MenuItemNotFoundException(String menuItemId) {
        super("Menu item not found with id: " + menuItemId);
    }
}