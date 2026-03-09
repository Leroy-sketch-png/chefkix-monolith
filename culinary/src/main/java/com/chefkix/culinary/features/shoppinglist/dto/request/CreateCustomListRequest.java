package com.chefkix.culinary.features.shoppinglist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateCustomListRequest {
    @NotBlank @Size(max = 100) String name;
}
