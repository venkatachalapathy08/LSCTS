package com.project.lscts.dto.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 100 , message = "title shouldn't exceed 100")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description shouldn't exceed 5000")
    private String description;
}
