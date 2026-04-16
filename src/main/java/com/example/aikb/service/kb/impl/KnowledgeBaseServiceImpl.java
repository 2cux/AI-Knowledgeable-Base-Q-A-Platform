package com.example.aikb.service.kb.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.dto.kb.KnowledgeBaseCreateRequest;
import com.example.aikb.dto.kb.KnowledgeBasePageRequest;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.kb.KnowledgeBaseService;
import com.example.aikb.vo.kb.KnowledgeBaseVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库业务实现类，负责按当前登录用户隔离知识库数据。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 创建当前用户的知识库，并返回创建后的展示信息。
     *
     * @param request 知识库创建请求参数
     * @return 创建后的知识库信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO create(KnowledgeBaseCreateRequest request) {
        Long userId = CurrentUser.getUserId();

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.getName().trim());
        knowledgeBase.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        knowledgeBase.setOwnerId(userId);
        knowledgeBase.setStatus(1);

        int rows = knowledgeBaseMapper.insert(knowledgeBase);
        if (rows != 1 || knowledgeBase.getId() == null) {
            throw new BusinessException("知识库创建失败");
        }

        KnowledgeBase saved = knowledgeBaseMapper.selectById(knowledgeBase.getId());
        return toVO(saved);
    }

    /**
     * 分页查询当前用户拥有的知识库列表。
     *
     * @param request 分页请求参数
     * @return 知识库分页结果
     */
    @Override
    public PageResult<KnowledgeBaseVO> page(KnowledgeBasePageRequest request) {
        Long userId = CurrentUser.getUserId();
        Page<KnowledgeBase> page = Page.of(request.getPageNum(), request.getPageSize());

        IPage<KnowledgeBase> result = knowledgeBaseMapper.selectPage(page, new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, 1)
                .orderByDesc(KnowledgeBase::getCreatedAt));

        List<KnowledgeBaseVO> list = result.getRecords()
                .stream()
                .map(this::toVO)
                .toList();

        return PageResult.<KnowledgeBaseVO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(request.getPageNum())
                .pageSize(request.getPageSize())
                .build();
    }

    /**
     * 查询当前用户指定知识库详情，不允许访问其他用户的知识库。
     *
     * @param id 知识库 ID
     * @return 知识库详情
     */
    @Override
    public KnowledgeBaseVO getById(Long id) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, id)
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, 1)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "知识库不存在");
        }
        return toVO(knowledgeBase);
    }

    /**
     * 将知识库实体转换为接口响应对象。
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库响应对象
     */
    private KnowledgeBaseVO toVO(KnowledgeBase knowledgeBase) {
        return KnowledgeBaseVO.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .status(knowledgeBase.getStatus())
                .createdAt(knowledgeBase.getCreatedAt())
                .updatedAt(knowledgeBase.getUpdatedAt())
                .build();
    }
}
