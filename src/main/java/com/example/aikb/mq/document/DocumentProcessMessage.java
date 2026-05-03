package com.example.aikb.mq.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessMessage {

    private Long documentId;

    private Long knowledgeBaseId;

    private Long userId;

    private Boolean force;

    private String requestId;

    private LocalDateTime createdAt;

    private Long taskId;

    private Integer chunkSize;

    private Integer overlap;

    private String textContent;
}
