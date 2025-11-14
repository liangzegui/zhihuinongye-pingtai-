# -Spring-Boot-Vue-
毕业设计
最后一步：验证上传结果（可选，确保放心）
打开你的 GitHub 仓库地址：https://github.com/liangzegui/-Spring-Boot-Vue-.git刷新页面后，就能看到你本地「毕业设计项目」里的所有文件（比如idea、vue相关文件夹 / 文件），和远程原本的README.md等文件一起存在，说明上传完全成功！
后续简化操作（下次更新代码时用）
之后本地修改文件后，无需再配置代理、关联仓库，直接执行 3 步即可推送更新：
bash
git add .  # 添加所有修改的文件
git commit -m "更新说明（比如：修复XX功能、添加XX文件）"
git push  # 直接推送（无需再写 -u origin main，已建立跟踪）
# 农业环境监控后端 (Agri-Backend)

一个基于 Spring Boot 的后端服务，用于智慧农业环境监控与数据分析平台。支持实时数据采集、历史查询、趋势分析和智能预警。

[<image-card alt="Java Version" src="https://img.shields.io/badge/Java-17-blue" ></image-card>](https://www.oracle.com/java/)
[<image-card alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.x-green" ></image-card>](https://spring.io/projects/spring-boot)
<image-card alt="License" src="https://img.shields.io/badge/License-MIT-yellow" ></image-card>

## 项目描述

这个后端项目是智慧农业系统的核心，提供 RESTful API 用于：
- 用户认证（登录/注册，使用验证码）。
- 实时环境监控（温度、湿度、土壤湿度、光照、CO2）。
- 历史数据查询和导出。
- 数据分析（趋势图、统计报表）。
- 智能预警（阈值检查、日志记录）。

技术栈：
- Spring Boot + MyBatis-Plus（数据库操作）
- MySQL（数据库）
- Hutool（工具库）
- JWT/Captcha（安全认证）

## 要求

- Java JDK 17 或更高
- Maven 3.8 或更高
- MySQL 8.0 或更高
- Git

## 依赖

查看 `pom.xml` 文件，关键依赖包括：
- spring-boot-starter-web
- mybatis-plus-boot-starter
- mysql-connector-java
- lombok
- hutool-all
- jjwt

## 构建项目

克隆仓库并使用 Maven 构建：

```bash
git clone https://github.com/your-username/agri-backend.git
cd agri-backend
mvn clean install
