# RAG主链路测试测试结果

## 测试链路：
从开始到结尾一整条RAG链路
主要包括了：用户注册->用户登入->创建知识库->上传文档->文档切片处理->文档向量化处理->检索文档->ai回答

## 用了什么测试文件
我使用了Apifox客户端来测试我的接口，而不是在 vscode 编写文件测试接口
Apifox是一个可视化的专门用来测试接口的软件，借助它能快速地测试接口

## 哪几个接口通过测试
1. /auth/register，注册接口
2. /auth/login，登入接口
3. /api/documents/upload-file，上传文档接口
4. /api/kb，创建知识库接口
5. /api/documents/{{documentId}}/process，文档切片处理接口
6. /api/documents/{{documentId}}/embed，文档向量化处理接口
7. /api/retrieval/search，检索接口
8. /api/chat/ask，问答接口

## 其他说明
- embedding模型和llm模型均生效
- retrieval能命中真实内容
- chat能基于citation真实回答，不会产生幻觉

## 存在的小问题或风险
创建数据库、分页查询数据库等功能是共用一个接口，可以优化