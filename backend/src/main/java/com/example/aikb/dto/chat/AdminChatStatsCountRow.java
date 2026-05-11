package com.example.aikb.dto.chat;

import lombok.Data;

@Data
public class AdminChatStatsCountRow {

    private Long totalChatCount;
    private Long matchedCount;
    private Long missedCount;
}
