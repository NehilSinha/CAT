package com.SRTS.CAT.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchCheckoutRequest {
    private List<String> equipmentIds;
    private String clientId;
    private String location;
    private String expectedReturnDate;
    private String siteId;
    private String operatorId;
}
