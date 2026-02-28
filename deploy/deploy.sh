#!/bin/bash
# ===========================================
# 智慧农业监控平台 - 一键部署脚本
# 适用于：CentOS 7/8, Ubuntu 20.04/22.04
# ===========================================

set -e

echo "=========================================="
echo "   智慧农业监控平台 - 服务器部署脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置变量（请根据实际情况修改）
MYSQL_ROOT_PASSWORD="Agri@2024"
MYSQL_DB_NAME="agri_db"
MYSQL_DB_USER="agri_user"
MYSQL_DB_PASSWORD="Agri@123456"
DOMAIN_OR_IP=""  # 留空则自动获取公网IP
GITHUB_REPO="https://github.com/liangzegui/zhihuinongye-pingtai-.git"
GITHUB_BRANCH="appmod/java-upgrade-20251226125202"

# 获取公网IP
get_public_ip() {
    curl -s ifconfig.me || curl -s icanhazip.com || echo "获取失败"
}

# 检测系统类型
detect_os() {
    if [ -f /etc/redhat-release ]; then
        OS="centos"
        PKG_MANAGER="yum"
    elif [ -f /etc/lsb-release ]; then
        OS="ubuntu"
        PKG_MANAGER="apt"
    else
        echo -e "${RED}不支持的操作系统${NC}"
        exit 1
    fi
    echo -e "${GREEN}检测到系统: $OS${NC}"
}

# 安装基础软件
install_base() {
    echo -e "${YELLOW}[1/7] 安装基础软件...${NC}"
    if [ "$OS" == "centos" ]; then
        yum update -y
        yum install -y wget curl git unzip vim
    else
        apt update -y
        apt install -y wget curl git unzip vim
    fi
}

# 安装 JDK 21
install_java() {
    echo -e "${YELLOW}[2/7] 安装 JDK 21...${NC}"
    if [ "$OS" == "centos" ]; then
        yum install -y java-21-openjdk java-21-openjdk-devel
    else
        apt install -y openjdk-21-jdk
    fi
    java -version
    echo -e "${GREEN}JDK 21 安装完成${NC}"
}

# 安装 Maven
install_maven() {
    echo -e "${YELLOW}[3/7] 安装 Maven...${NC}"
    cd /opt
    wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
    tar -xzf apache-maven-3.9.6-bin.tar.gz
    ln -sf /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn
    mvn -version
    echo -e "${GREEN}Maven 安装完成${NC}"
}

# 安装 Node.js
install_nodejs() {
    echo -e "${YELLOW}[4/7] 安装 Node.js 18...${NC}"
    if [ "$OS" == "centos" ]; then
        curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
        yum install -y nodejs
    else
        curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
        apt install -y nodejs
    fi
    node -v
    npm -v
    echo -e "${GREEN}Node.js 安装完成${NC}"
}

# 安装 MySQL 8
install_mysql() {
    echo -e "${YELLOW}[5/7] 安装 MySQL 8...${NC}"
    if [ "$OS" == "centos" ]; then
        yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
        yum install -y mysql-community-server
        systemctl start mysqld
        systemctl enable mysqld
        
        # 获取临时密码
        TEMP_PASSWORD=$(grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
        
        # 修改密码并创建数据库
        mysql -uroot -p"$TEMP_PASSWORD" --connect-expired-password <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PASSWORD';
CREATE DATABASE IF NOT EXISTS $MYSQL_DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$MYSQL_DB_USER'@'localhost' IDENTIFIED BY '$MYSQL_DB_PASSWORD';
GRANT ALL PRIVILEGES ON $MYSQL_DB_NAME.* TO '$MYSQL_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF
    else
        apt install -y mysql-server
        systemctl start mysql
        systemctl enable mysql
        
        # 创建数据库和用户
        mysql -uroot <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '$MYSQL_ROOT_PASSWORD';
CREATE DATABASE IF NOT EXISTS $MYSQL_DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$MYSQL_DB_USER'@'localhost' IDENTIFIED BY '$MYSQL_DB_PASSWORD';
GRANT ALL PRIVILEGES ON $MYSQL_DB_NAME.* TO '$MYSQL_DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF
    fi
    echo -e "${GREEN}MySQL 安装完成${NC}"
}

# 安装 Nginx
install_nginx() {
    echo -e "${YELLOW}[6/7] 安装 Nginx...${NC}"
    if [ "$OS" == "centos" ]; then
        yum install -y nginx
    else
        apt install -y nginx
    fi
    systemctl start nginx
    systemctl enable nginx
    echo -e "${GREEN}Nginx 安装完成${NC}"
}

# 部署项目
deploy_project() {
    echo -e "${YELLOW}[7/7] 部署项目...${NC}"
    
    # 克隆代码
    cd /opt
    rm -rf agri-project
    git clone -b $GITHUB_BRANCH $GITHUB_REPO agri-project
    
    # 部署后端
    echo "构建后端..."
    cd /opt/agri-project/idea/agri-backend
    
    # 修改数据库配置
    cat > src/main/resources/application-prod.properties <<EOF
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/$MYSQL_DB_NAME?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=$MYSQL_DB_USER
spring.datasource.password=$MYSQL_DB_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
server.servlet.session.timeout=1800
EOF
    
    mvn clean package -DskipTests
    
    # 创建后端服务
    cat > /etc/systemd/system/agri-backend.service <<EOF
[Unit]
Description=Agri Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/agri-project/idea/agri-backend
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod target/agri-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
    
    systemctl daemon-reload
    systemctl start agri-backend
    systemctl enable agri-backend
    
    # 部署前端
    echo "构建前端..."
    cd /opt/agri-project/vue/agri-frontend
    
    # 获取公网IP
    if [ -z "$DOMAIN_OR_IP" ]; then
        DOMAIN_OR_IP=$(get_public_ip)
    fi
    
    # 设置生产环境API地址
    echo "VUE_APP_API_BASE_URL=http://$DOMAIN_OR_IP:8080" > .env.production
    
    npm install
    npm run build
    
    # 复制前端文件到Nginx目录
    rm -rf /usr/share/nginx/html/*
    cp -r dist/* /usr/share/nginx/html/
    
    # 配置Nginx
    cat > /etc/nginx/conf.d/agri.conf <<EOF
server {
    listen 80;
    server_name $DOMAIN_OR_IP;
    
    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }
    
    # 后端API代理
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
EOF
    
    # 重启Nginx
    nginx -t && systemctl restart nginx
    
    echo -e "${GREEN}项目部署完成！${NC}"
}

# 初始化数据库表
init_database() {
    echo "初始化数据库..."
    mysql -u$MYSQL_DB_USER -p$MYSQL_DB_PASSWORD $MYSQL_DB_NAME <<EOF
-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 环境数据表
CREATE TABLE IF NOT EXISTS t_env_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sensor_id INT,
    temperature DOUBLE,
    humidity DOUBLE,
    soil_moisture DOUBLE,
    light_intensity INT,
    co2 INT,
    collect_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_collect_time (collect_time)
);

-- 预警日志表
CREATE TABLE IF NOT EXISTS t_warning_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warning_type VARCHAR(50),
    warning_level VARCHAR(20),
    warning_message VARCHAR(500),
    sensor_value DOUBLE,
    threshold_value DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_handled TINYINT DEFAULT 0,
    INDEX idx_create_time (create_time)
);

-- 插入测试用户 (密码: 123456)
INSERT IGNORE INTO t_user (username, password, phone, email) 
VALUES ('admin', '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', 'admin@example.com');
EOF
    echo -e "${GREEN}数据库初始化完成${NC}"
}

# 显示完成信息
show_info() {
    PUBLIC_IP=$(get_public_ip)
    echo ""
    echo "=========================================="
    echo -e "${GREEN}   部署完成！${NC}"
    echo "=========================================="
    echo ""
    echo "访问地址："
    echo "  前端: http://$PUBLIC_IP"
    echo "  后端: http://$PUBLIC_IP:8080"
    echo ""
    echo "数据库信息："
    echo "  数据库名: $MYSQL_DB_NAME"
    echo "  用户名: $MYSQL_DB_USER"
    echo "  密码: $MYSQL_DB_PASSWORD"
    echo ""
    echo "华为云IoTDA配置："
    echo "  Webhook地址: http://$PUBLIC_IP:8080/api/iotda/webhook"
    echo ""
    echo "服务管理："
    echo "  后端服务: systemctl status/start/stop/restart agri-backend"
    echo "  Nginx: systemctl status/start/stop/restart nginx"
    echo "  MySQL: systemctl status/start/stop/restart mysql"
    echo ""
    echo "⚠️ 请确保云服务器安全组已开放端口: 80, 8080"
    echo "=========================================="
}

# 主流程
main() {
    detect_os
    install_base
    install_java
    install_maven
    install_nodejs
    install_mysql
    install_nginx
    deploy_project
    init_database
    show_info
}

main
