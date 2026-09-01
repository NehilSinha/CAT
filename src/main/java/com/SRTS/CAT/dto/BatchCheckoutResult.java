package com.SRTS.CAT.dto;

import com.SRTS.CAT.entity.EquipmentEntry;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class BatchCheckoutResult {
    private List<EquipmentEntry> checkedOut;
    private Map<String, String> failed;
}
