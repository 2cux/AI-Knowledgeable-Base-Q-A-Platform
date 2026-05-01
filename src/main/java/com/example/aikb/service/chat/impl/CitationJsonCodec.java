package com.example.aikb.service.chat.impl;

import com.example.aikb.exception.BusinessException;
import com.example.aikb.vo.chat.CitationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Serializes and parses chat record citations stored in chat_record.citations_json.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CitationJsonCodec {

    private static final TypeReference<List<CitationVO>> CITATION_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * Converts citations to JSON before persisting an ask record.
     */
    String serialize(List<CitationVO> citations) {
        try {
            return objectMapper.writeValueAsString(citations == null ? Collections.emptyList() : citations);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50001, "\u5f15\u7528\u6765\u6e90\u5e8f\u5217\u5316\u5931\u8d25");
        }
    }

    /**
     * Parses persisted citations for record detail responses; invalid history data returns an empty list.
     */
    List<CitationVO> deserialize(String citationsJson, Long recordId) {
        if (citationsJson == null || citationsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<CitationVO> citations = objectMapper.readValue(citationsJson, CITATION_LIST_TYPE);
            return citations == null ? Collections.emptyList() : citations;
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse chat record citations, recordId={}, error={}", recordId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
