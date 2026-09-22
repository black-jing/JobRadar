package com.jobradar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XiaozhaoRadarJobDto {

    public String c;    // 公司
    public String p;    // 岗位
    public String l;    // 地点
    public String e;
    public String w;    // 批次
    public String d;    // 截止日期
    public String s;
    public String t;
    public String ind;  // 行业
    public String u;    // 投递链接
}