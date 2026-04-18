package com.example.aikb.service.document;

import com.example.aikb.entity.Document;
import com.example.aikb.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MVP 占位文档解析器：当前仅接受请求传入的纯文本，真实文件解析后续由受控存储模块接入。
 */
@Component
public class SimpleDocumentParser {

    /**
     * 获取文档纯文本内容。
     *
     * @param document 文档实体，预留给后续按文件类型选择真实解析器
     * @param textContent 请求直接传入的纯文本
     * @return 文档纯文本
     */
    public String parse(Document document, String textContent) {
        if (StringUtils.hasText(textContent)) {
            return textContent.trim();
        }
        throw new BusinessException("MVP阶段请在process请求中传入textContent");
    }
}
