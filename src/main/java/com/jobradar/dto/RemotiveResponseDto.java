package com.jobradar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemotiveResponseDto {

    public List<RemotiveJobDto> jobs;
}