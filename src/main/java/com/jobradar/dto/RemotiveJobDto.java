package com.jobradar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemotiveJobDto {

    public String url;
    public String title;
    public String company_name;
    public String publication_date;
    public String candidate_required_location;
    public String description;
}