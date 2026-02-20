package com.steffaneleal.lunnar.dto;

import java.util.UUID;

public record AddressDTO(UUID id, String street, String number, String complement, String neighborhood, String city, String state, String zipCode) {
}
