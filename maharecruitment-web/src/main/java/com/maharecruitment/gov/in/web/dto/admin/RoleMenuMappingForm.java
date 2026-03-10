package com.maharecruitment.gov.in.web.dto.admin;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleMenuMappingForm {

    @NotNull(message = "Role is required")
    private Long roleId;

    private List<Long> menuIds = new ArrayList<>();
}
