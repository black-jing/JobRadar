package com.jobradar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XiaozhaoRadarResponseDto {

    public String updated;
    public int count;
    public List<XiaozhaoRadarJobDto> jobs;
}