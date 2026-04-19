目标
确定项目方向，完成项目总体设计，建立 AI coding 使用规范。
任务
1.  确定项目名和项目范围
 建议定为： 
企业级 AI 知识库问答后端平台
2.  明确第一版功能范围，只保留这些： 
-  用户登录/注册 
-  知识库创建 
-  文档上传 
-  文档切片入库 
-  基础问答接口 
-  会话记录 
-  问答历史 
-  管理端基础接口 
3.  设计数据库表
 至少先设计： 
-  user 
-  knowledge_base 
-  document 
-  document_chunk 
-  conversation 
-  message 
-  task_record 
4.  建立项目结构
 建议： 
-  controller 
-  service 
-  mapper/repository 
-  entity 
-  dto 
-  vo 
-  config 
-  common 
-  exception 
5.  建立 AI coding 工作流
 每天固定这样用： 
-  先自己写需求 
-  再让 AI 帮你拆接口 
-  再自己确认设计 
-  再让 AI 生成骨架 
-  最后自己改、跑、调试、记录问题 