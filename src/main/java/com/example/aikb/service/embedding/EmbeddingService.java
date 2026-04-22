package com.example.aikb.service.embedding;

import com.example.aikb.dto.embedding.request.EmbeddingInputItem;
import com.example.aikb.dto.embedding.request.EmbeddingRequest;
import java.util.List;

/**
 * 最小 embedding 服务示例，面向业务代码暴露纯文本向量化方法。
 */
public interface EmbeddingService {

    /**
     * 使用配置中的默认模型向量化单段文本。
     *
     * @param text 待向量化文本
     * @return 向量结果
     */
    List<Float> embedText(String text);

    /**
     * 使用指定模型向量化单段文本。
     *
     * @param text 待向量化文本
     * @param model embedding 模型名称
     * @return 向量结果
     */
    List<Float> embedText(String text, String model);

    /**
     * 使用指定模型向量化单段文本和可选图片输入。
     *
     * @param text 待向量化文本
     * @param image 图片内容或图片地址；纯文本调用时传空
     * @param model embedding 模型名称
     * @return 向量结果
     */
    List<Float> embedText(String text, String image, String model);

    /**
     * 使用指定模型按上游协议透传 input 数组。
     *
     * @param input 上游 input 数组，元素包含 text/image
     * @param model embedding 模型名称
     * @return 向量结果
     */
    List<Float> embedInput(List<EmbeddingInputItem> input, String model);

    /**
     * 按上游 embedding 请求体执行向量化，主要用于联调时透传完整协议字段。
     *
     * @param request embedding 上游请求体
     * @return 向量结果
     */
    List<Float> embedRequest(EmbeddingRequest request);
}
