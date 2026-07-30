package com.example.commerce.logistics.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TraceResult {

    private String trackingNo;
    private String carrier;
    /** 最新一条轨迹内容 */
    private String lastTrace;
    private List<TraceItem> traces;

    @Data
    public static class TraceItem {
        private LocalDateTime time;
        private String content;
    }
}
