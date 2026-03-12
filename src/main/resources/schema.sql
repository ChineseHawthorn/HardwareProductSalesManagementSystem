-- 五金产品销售管理系统 数据库建表脚本
-- 数据库：hardware
-- 字符集：utf8mb4
-- 创建时间：2026-03-03

CREATE DATABASE IF NOT EXISTS hardware DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hardware;

-- ===================== 角色表 =====================
CREATE TABLE IF NOT EXISTS `role` (
    `role_id`   INT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    `role_name` VARCHAR(20) NOT NULL COMMENT '角色名称（店主/店员/消费者）',
    `role_type` TINYINT NOT NULL DEFAULT 0 COMMENT '角色类型：0=店主，1=店员，2消费者',
    `role_desc` VARCHAR(200) COMMENT '角色描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ===================== 角色权限表 =====================
CREATE TABLE IF NOT EXISTS `role_permission` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `role_id`         INT NOT NULL COMMENT '关联角色ID',
    `permission_code` VARCHAR(50) NOT NULL COMMENT '权限码（如 product:add）',
    `permission_name` VARCHAR(100) COMMENT '权限名称',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_role_id (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限表';

-- ===================== 用户表 =====================
CREATE TABLE IF NOT EXISTS `user` (
    `user_id`         INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username`        VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
    `password`        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `avatar`          VARCHAR(200) DEFAULT './images/avatar/default.png' COMMENT '头像URL',
    `role_id`         INT NOT NULL COMMENT '关联角色ID',
    `real_name`       VARCHAR(50) COMMENT '真实姓名',
    `phone`           CHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    INDEX idx_role_id (`role_id`),
    INDEX idx_phone (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ===================== 商品分类表 =====================
CREATE TABLE IF NOT EXISTS `product_category` (
    `category_id`   INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    `parent_id`     INT NOT NULL DEFAULT 0 COMMENT '父分类ID（0表示顶级）',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
    `sort_order`    INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (`parent_id`),
    UNIQUE KEY uk_parent_name (`parent_id`, `category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ===================== 商品表 =====================
CREATE TABLE IF NOT EXISTS `product` (
    `product_id`   INT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `brand`        VARCHAR(50) COMMENT '品牌',
    `model`        VARCHAR(100) COMMENT '型号',
    `category_id`  INT COMMENT '关联分类ID',
    `description`  TEXT COMMENT '商品描述',
    `is_on_sale`   TINYINT NOT NULL DEFAULT 1 COMMENT '上架状态：0=下架，1=上架',
    `create_user`  INT COMMENT '创建人user_id',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_id (`category_id`),
    FULLTEXT INDEX ft_product_name (`product_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ===================== 商品规格表 =====================
CREATE TABLE IF NOT EXISTS `product_spec` (
    `spec_id`          INT AUTO_INCREMENT PRIMARY KEY COMMENT '规格ID',
    `product_id`       INT NOT NULL COMMENT '关联商品ID',
    `material`         VARCHAR(50) COMMENT '材质',
    `size`             VARCHAR(100) COMMENT '尺寸/规格描述',
    `unit_price`       DECIMAL(10,2) NOT NULL COMMENT '零售单价',
    `unit`             VARCHAR(20) DEFAULT '个' COMMENT '单位',
    `stock_quantity`   INT NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    `warning_threshold` INT NOT NULL DEFAULT 10 COMMENT '库存预警阈值',
    `spec_code`        VARCHAR(50) NOT NULL UNIQUE COMMENT '规格编码（唯一）',
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (`product_id`),
    INDEX idx_spec_code (`spec_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- ===================== 商品图片表 =====================
CREATE TABLE IF NOT EXISTS `product_image` (
    `image_id`    INT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    `product_id`  INT NOT NULL COMMENT '关联商品ID',
    `image_url`   VARCHAR(500) NOT NULL COMMENT '图片URL',
    `image_sort`  TINYINT NOT NULL DEFAULT 2 COMMENT '排序：1=主图，2-5=副图',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0=正常，1=已删除',
    INDEX idx_product_id (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- ===================== 价格策略表 =====================
CREATE TABLE IF NOT EXISTS `price_strategy` (
    `strategy_id`     INT AUTO_INCREMENT PRIMARY KEY COMMENT '策略ID',
    `strategy_name`   VARCHAR(100) NOT NULL COMMENT '策略名称',
    `strategy_type`   TINYINT NOT NULL COMMENT '策略类型：1=按规格，2=按分类，3=全局',
    `product_id`      INT COMMENT '关联商品ID（策略类型=1时有效）',
    `category_id`     INT COMMENT '关联分类ID（策略类型=2时有效）',
    `min_quantity`    INT NOT NULL DEFAULT 1 COMMENT '最小购买数量',
    `discount_type`   TINYINT NOT NULL COMMENT '折扣类型：1=折扣比例，2=固定减额，3=固定价格',
    `discount_value`  DECIMAL(10,2) NOT NULL COMMENT '折扣值',
    `start_time`      DATETIME COMMENT '开始时间',
    `end_time`        DATETIME COMMENT '结束时间（NULL=永久有效）',
    `applicable_role` TINYINT NOT NULL DEFAULT 2 COMMENT '适用角色：0=个人，1=企业，2=全部',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    `creator`         INT COMMENT '创建人user_id',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格策略表';

-- ===================== 供应商表 =====================
CREATE TABLE IF NOT EXISTS `supplier` (
    `supplier_id`       INT AUTO_INCREMENT PRIMARY KEY COMMENT '供应商ID',
    `supplier_name`     VARCHAR(100) NOT NULL UNIQUE COMMENT '供应商名称（唯一）',
    `contact_person`    VARCHAR(50) COMMENT '联系人',
    `contact_phone`     CHAR(11) NOT NULL UNIQUE COMMENT '联系电话（11位，唯一）',
    `supplier_addr`     VARCHAR(200) COMMENT '供应商地址',
    `qualification_url` VARCHAR(500) COMMENT '资质证书URL',
    `status`            TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `operator`          INT COMMENT '操作人user_id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- ===================== 入库单表 =====================
CREATE TABLE IF NOT EXISTS `stock_in_order` (
    `in_order_id`    VARCHAR(20) PRIMARY KEY COMMENT '入库单号（IN+YYYYMMDD+6位随机数）',
    `in_date`        DATETIME NOT NULL COMMENT '入库日期',
    `total_amount`   DECIMAL(12,2) COMMENT '入库总金额',
    `certificate_url` VARCHAR(500) COMMENT '凭证图片URL',
    `operator`       INT COMMENT '操作人user_id',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `remark`         VARCHAR(500) COMMENT '备注',
    `audit_status`   TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核，1=已通过，2=已拒绝',
    `supplier_id`    INT COMMENT '关联供应商ID',
    INDEX idx_audit_status (`audit_status`),
    INDEX idx_supplier_id (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- ===================== 入库单明细表 =====================
CREATE TABLE IF NOT EXISTS `stock_in_detail` (
    `detail_id`      INT AUTO_INCREMENT PRIMARY KEY,
    `in_order_id`    VARCHAR(20) NOT NULL COMMENT '关联入库单号',
    `spec_id`        INT NOT NULL COMMENT '关联规格ID',
    `in_quantity`    INT NOT NULL COMMENT '入库数量（≥1）',
    `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '进货单价',
    `subtotal`       DECIMAL(12,2) COMMENT '小计金额',
    INDEX idx_in_order_id (`in_order_id`),
    INDEX idx_spec_id (`spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- ===================== 出库单表 =====================
CREATE TABLE IF NOT EXISTS `stock_out_order` (
    `out_order_id`    VARCHAR(20) PRIMARY KEY COMMENT '出库单号（OUT+YYYYMMDD+6位随机数）',
    `out_type`        TINYINT NOT NULL COMMENT '出库类型：1=销售，2=损耗，3=调拨',
    `out_date`        DATETIME NOT NULL COMMENT '出库日期',
    `total_quantity`  INT COMMENT '出库总数量',
    `related_order_id` VARCHAR(25) COMMENT '关联订单号',
    `certificate_url`  VARCHAR(500) COMMENT '凭证URL',
    `operator`        INT COMMENT '操作人user_id',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `remark`          VARCHAR(500) COMMENT '备注',
    `audit_status`    TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核，1=已通过，2=已拒绝',
    INDEX idx_out_type (`out_type`),
    INDEX idx_audit_status (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- ===================== 出库单明细表 =====================
CREATE TABLE IF NOT EXISTS `stock_out_detail` (
    `detail_id`   INT AUTO_INCREMENT PRIMARY KEY,
    `out_order_id` VARCHAR(20) NOT NULL COMMENT '关联出库单号',
    `spec_id`     INT NOT NULL COMMENT '关联规格ID',
    `out_quantity` INT NOT NULL COMMENT '出库数量',
    `out_price`   DECIMAL(10,2) COMMENT '出库单价',
    `subtotal`    DECIMAL(12,2) COMMENT '小计金额',
    `batch_no`    VARCHAR(50) COMMENT '批次号',
    INDEX idx_out_order_id (`out_order_id`),
    INDEX idx_spec_id (`spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';

-- ===================== 库存变动日志表 =====================
CREATE TABLE IF NOT EXISTS `stock_log` (
    `log_id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `spec_id`          INT NOT NULL COMMENT '关联规格ID',
    `change_type`      TINYINT NOT NULL COMMENT '变动类型：1=入库，2=出库',
    `change_quantity`  INT NOT NULL COMMENT '变动数量（正=入库，负=出库）',
    `before_quantity`  INT NOT NULL COMMENT '变动前库存',
    `after_quantity`   INT NOT NULL COMMENT '变动后库存',
    `related_order_id` VARCHAR(25) COMMENT '关联单据ID',
    `operator`         INT COMMENT '操作人user_id',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_spec_id (`spec_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志表';

-- ===================== 订单表 =====================
CREATE TABLE IF NOT EXISTS `order` (
    `order_id`      VARCHAR(25) PRIMARY KEY COMMENT '订单号（ORDER+YYYYMMDD+6位随机数）',
    `user_id`       INT COMMENT '下单用户ID',
    `order_amount`  DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    `pay_status`    TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待支付，1=已支付，2=已取消，3=已退款，4=待发货，5=已完成',
    `order_type`    TINYINT NOT NULL DEFAULT 1 COMMENT '订单类型：1=线上，2=线下',
    `trade_type`    TINYINT NOT NULL DEFAULT 2 COMMENT '交易类型：1=批发，2=零售',
    `receiver_name` VARCHAR(50) COMMENT '收货人姓名',
    `receiver_phone` CHAR(11) COMMENT '收货人电话',
    `receiver_addr` VARCHAR(200) COMMENT '收货地址（线下为NULL）',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `pay_time`      DATETIME COMMENT '支付时间',
    `remark`        VARCHAR(500) COMMENT '备注',
    INDEX idx_user_id (`user_id`),
    INDEX idx_pay_status (`pay_status`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ===================== 订单明细表 =====================
CREATE TABLE IF NOT EXISTS `order_detail` (
    `detail_id`    INT AUTO_INCREMENT PRIMARY KEY,
    `order_id`     VARCHAR(25) NOT NULL COMMENT '关联订单号',
    `spec_id`      INT NOT NULL COMMENT '关联规格ID',
    `sale_quantity` INT NOT NULL COMMENT '销售数量',
    `sale_price`   DECIMAL(10,2) NOT NULL COMMENT '实际售价',
    `subtotal`     DECIMAL(12,2) COMMENT '小计金额',
    `is_refunded`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否已退款：0=否，1=是',
    INDEX idx_order_id (`order_id`),
    INDEX idx_spec_id (`spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ===================== 购物车表 =====================
CREATE TABLE IF NOT EXISTS `cart` (
    `cart_id`     INT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车记录ID',
    `user_id`     INT NOT NULL COMMENT '用户ID',
    `spec_id`     INT NOT NULL COMMENT '商品规格ID',
    `quantity`    INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_spec (`user_id`, `spec_id`) COMMENT '同一用户同一规格只保留一条',
    INDEX idx_user_id (`user_id`),
    INDEX idx_spec_id (`spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ===================== 收藏表 =====================
CREATE TABLE IF NOT EXISTS `favorite` (
    `favorite_id`  INT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    `user_id`      INT NOT NULL COMMENT '用户ID',
    `product_id`   INT NOT NULL COMMENT '商品ID',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_product (`user_id`, `product_id`) COMMENT '同一用户同一商品只收藏一次',
    INDEX idx_user_id (`user_id`),
    INDEX idx_product_id (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

-- ===================== 支付记录表 =====================
CREATE TABLE IF NOT EXISTS `payment_record` (
    `pay_id`         INT AUTO_INCREMENT PRIMARY KEY COMMENT '支付记录ID',
    `order_id`       VARCHAR(25) NOT NULL COMMENT '关联订单号',
    `pay_amount`     DECIMAL(12,2) NOT NULL COMMENT '实际支付金额',
    `pay_method`     TINYINT NOT NULL COMMENT '支付方式：1=微信，2=支付宝，3=现金',
    `transaction_id` VARCHAR(100) COMMENT '第三方交易流水号',
    `pay_status`     TINYINT NOT NULL DEFAULT 1 COMMENT '支付状态：1=成功，2=失败',
    `operator`       INT COMMENT '操作人user_id',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ===================== 退款记录表 =====================
CREATE TABLE IF NOT EXISTS `refund_record` (
    `refund_id`     INT AUTO_INCREMENT PRIMARY KEY COMMENT '退款记录ID',
    `order_id`      VARCHAR(25) NOT NULL COMMENT '关联订单号',
    `pay_id`        INT COMMENT '关联支付记录ID',
    `refund_amount` DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    `refund_reason` VARCHAR(500) COMMENT '退款原因',
    `refund_method` TINYINT NOT NULL COMMENT '退款方式：1=原路退回，2=现金退款',
    `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态：0=待审核，1=成功，2=失败',
    `operator`      INT COMMENT '操作人user_id',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (`order_id`),
    INDEX idx_refund_status (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- ===================== 用户反馈表 =====================
CREATE TABLE IF NOT EXISTS `feedback` (
    `feedback_id`  INT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
    `user_id`      INT COMMENT '提交人user_id（可为null匿名）',
    `title`        VARCHAR(100) NOT NULL COMMENT '反馈标题',
    `content`      TEXT NOT NULL COMMENT '反馈内容',
    `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=未回复，1=已回复，2=已关闭',
    `product_id`   INT COMMENT '关联商品ID（可选）',
    `order_id`     VARCHAR(50) COMMENT '关联订单ID（可选）',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- ===================== 反馈回复表 =====================
CREATE TABLE IF NOT EXISTS `feedback_reply` (
    `reply_id`      INT AUTO_INCREMENT PRIMARY KEY COMMENT '回复ID',
    `feedback_id`   INT NOT NULL COMMENT '关联反馈ID',
    `reply_user_id` INT NOT NULL COMMENT '回复人user_id',
    `content`       TEXT NOT NULL COMMENT '回复内容',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_id (`feedback_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈回复表';

-- ===================== 客服会话表 =====================
CREATE TABLE IF NOT EXISTS `chat_session` (
    `session_id`   INT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    `consumer_id`  INT NOT NULL COMMENT '消费者用户ID',
    `staff_id`     INT DEFAULT NULL COMMENT '接待店员ID（null=未分配）',
    `title`        VARCHAR(100) NOT NULL DEFAULT '客服咨询' COMMENT '会话标题',
    `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待接入，1=进行中，2=已关闭',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_consumer_id (`consumer_id`),
    INDEX idx_staff_id (`staff_id`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话表';

-- ===================== 聊天消息表 =====================
CREATE TABLE IF NOT EXISTS `chat_message` (
    `msg_id`      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `session_id`  INT NOT NULL COMMENT '关联会话ID',
    `sender_id`   INT NOT NULL COMMENT '发送者用户ID',
    `sender_role` TINYINT NOT NULL DEFAULT 0 COMMENT '发送者角色：0=消费者，1=店员/店主',
    `content`     TEXT NOT NULL COMMENT '消息内容',
    `msg_type`    TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型：0=文字，1=图片',
    `is_read`     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_session_id (`session_id`),
    INDEX idx_sender_id (`sender_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ===================== 初始数据 =====================

-- 初始化角色（role_id: 0=店主, 1=店员, 2=消费者）
INSERT INTO `role` (`role_id`, `role_name`, `role_type`, `role_desc`) VALUES
(0, '店主', 0, '系统管理员，拥有全部权限'),
(1, '店员', 1, '店铺员工，负责日常销售和库存操作'),
(2, '消费者', 2, '普通个人消费者，可浏览商品、下单、支付');


-- 初始化店主账号（密码：admin123，BCrypt加密）
INSERT INTO `user` (`username`, `password`, `role_id`, `real_name`, `phone`, `status`, `create_time`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6B/P2', 0, '系统管理员', '13800000001', 1, NOW());

-- 初始化商品一级分类（INSERT IGNORE 防止重复运行schema时重复插入）
INSERT IGNORE INTO `product_category` (`parent_id`, `category_name`, `status`, `sort_order`) VALUES
(0, '建筑五金', 1, 1),
(0, '工具五金', 1, 2),
(0, '日用五金', 1, 3),
(0, '工业五金', 1, 4);

-- 子分类（parent_id对应上方一级分类自增ID，首次插入时从1开始）
INSERT IGNORE INTO `product_category` (`parent_id`, `category_name`, `status`, `sort_order`) VALUES
-- 建筑五金子分类
(1, '门窗五金', 1, 1),
(1, '水暖五金', 1, 2),
(1, '结构五金', 1, 3),
-- 工具五金子分类
(2, '手动工具', 1, 1),
(2, '电动工具', 1, 2),
(2, '量具量仪', 1, 3),
-- 日用五金子分类
(3, '厨房五金', 1, 1),
(3, '卫浴五金', 1, 2),
(3, '家居五金', 1, 3),
-- 工业五金子分类
(4, '机械五金', 1, 1),
(4, '模具五金', 1, 2),
(4, '特种五金', 1, 3);

-- 初始化店员角色权限
INSERT INTO `role_permission` (`role_id`, `permission_code`, `permission_name`) VALUES
(1, 'product:view', '查看商品'),
(1, 'product:status', '修改商品状态'),
(1, 'inventory:view', '查看库存'),
(1, 'inventory:in:add', '提交入库申请'),
(1, 'inventory:out:add', '提交出库申请'),
(1, 'order:create', '创建订单'),
(1, 'order:ship', '标记发货'),
(1, 'supplier:view', '查看供应商');

-- 初始化店主角色权限（全部权限）
INSERT INTO `role_permission` (`role_id`, `permission_code`, `permission_name`) VALUES
(0, 'product:add', '添加商品'),
(0, 'product:edit', '编辑商品'),
(0, 'product:delete', '删除商品'),
(0, 'product:view', '查看商品'),
(0, 'product:status', '修改商品状态'),
(0, 'product:category:manage', '分类管理'),
(0, 'product:price:manage', '价格策略管理'),
(0, 'inventory:view', '查看库存'),
(0, 'inventory:in:manage', '入库管理'),
(0, 'inventory:out:manage', '出库管理'),
(0, 'inventory:log:view', '查看库存日志'),
(0, 'inventory:adjust', '库存调整'),
(0, 'order:create', '创建订单'),
(0, 'order:manage', '订单管理'),
(0, 'order:refund:manage', '退款管理'),
(0, 'supplier:manage', '供应商管理'),
(0, 'user:consumer:manage', '消费者管理'),
(0, 'user:staff:manage', '店员管理'),
(0, 'stat:view', '数据统计');

-- ===================== 供应商初始数据 =====================
INSERT INTO `supplier` (`supplier_name`, `contact_person`, `contact_phone`, `supplier_addr`, `status`) VALUES
('上海标准五金有限公司',   '张经理', '13911110001', '上海市嘉定区工业园区标准路88号',       1),
('广州工具制造商贸公司',   '李总',   '13811110002', '广东省广州市番禺区工具产业园区22号',   1),
('佛山不锈钢制品厂',       '陈厂长', '13711110003', '广东省佛山市南海区里水镇工业路66号',   1),
('宁波精密五金制造有限公司','王工',  '13611110004', '浙江省宁波市鄞州区精密工业园6号楼',   1),
('成都建材五金批发中心',   '刘老板', '13511110005', '四川省成都市金牛区建材市场B区88号',   1);

-- ===================== 商品初始数据（13件，对应13张硬件图片） =====================
INSERT INTO `product` (`product_name`, `brand`, `model`, `category_id`, `description`, `is_on_sale`, `create_user`, `create_time`) VALUES
-- 门窗五金 (category_id=5)
('不锈钢合页',       '固力', '304合页',    5, '采用304不锈钢材质，耐腐蚀耐磨损，适用于各类门窗安装，承重能力强，开合顺畅，三种尺寸可选。', 1, 1, NOW()),
('球形门锁',         '固力', 'GL-5588',    5, '锌合金球形门锁，镀铬表面处理，防锈耐用，含两把钥匙，分室内标准款与加强款，适用于木门安装。', 1, 1, NOW()),
('铝合金推拉窗滑轮', '华辉', 'HH-002',    5, '铝合金外壳，双轴承滚轮，滑动顺畅无噪音，适用于各类推拉窗及推拉门安装更换，小号和大号可选。', 1, 1, NOW()),
-- 水暖五金 (category_id=6)
('单冷厨房水龙头',   '九牧', 'JM-001',    6, '304不锈钢材质水龙头，360°旋转出水，高光镜面处理，冷水款与冷热款可选，安装简便。', 1, 1, NOW()),
('PPR管道热熔接头',  '联塑', 'LS-PPR',    6, '联塑PPR热熔管道接头，耐高温高压，适用于家装给水管道连接，DN20/DN25/DN32三种管径可选。', 1, 1, NOW()),
('全铜球阀',         '埃美柯','AMC-DN',   6, '全铜锻造球阀，密封性好，启闭灵活，耐高压，适用于水管截止控制，DN15/DN20/DN25口径可选。', 1, 1, NOW()),
-- 结构五金 (category_id=7)
('不锈钢膨胀螺丝',   '金固', '304膨胀',   7, '304不锈钢材质膨胀螺丝，抗拉拔力强，耐腐蚀，适用于混凝土、砖墙等基材固定，50个/包，M6/M8/M10可选。', 1, 1, NOW()),
-- 手动工具 (category_id=8)
('活动扳手',         '史丹利','ST-BH',    8, '铬钒钢锻造活动扳手，可调节开口，双面齿纹，防滑软质手柄，适用于各类六角螺母拆装，6/8/12寸可选。', 1, 1, NOW()),
('螺丝刀套装',       '世达', 'SATA-09317',8, '铬钒钢刀头，磁性吸附，防滑双色软柄，十字/一字多规格套装，适用于家居日常维修，8件套和12件套可选。', 1, 1, NOW()),
-- 电动工具 (category_id=9)
('冲击电钻',         '博世', 'GSB13RE',   9, '博世600W冲击电钻，0~3000转/分可调速，正反转功能，适用于混凝土及砌体打孔，单机版与套装版可选。', 1, 1, NOW()),
-- 量具量仪 (category_id=10)
('钢卷尺',           '广陆', 'GL-5025',   10,'钢卷尺，宽25mm，尼龙涂层刻度耐磨，磁性钩头，自动锁定，配腰夹，3米/5米/7.5米三种长度可选。', 1, 1, NOW()),
-- 厨房五金 (category_id=11)
('不锈钢厨房水槽',   '九牧', 'JM-SC',    11,'304不锈钢水槽，拉丝哑光表面，含溢水孔，耐腐蚀耐高温，含落水器配件，单槽50/60cm及双槽80cm可选。', 1, 1, NOW()),
-- 卫浴五金 (category_id=12)
('不锈钢毛巾架',     '摩恩', 'MN-YB',    12,'304不锈钢双杆毛巾架，抛光镜面处理，防锈耐用，含膨胀螺丝安装配件，45cm单杆与60cm双杆可选。', 1, 1, NOW());

-- ===================== 商品规格初始数据 =====================
-- product_id 1~13 依次对应上方 13 件商品
INSERT INTO `product_spec` (`product_id`, `material`, `size`, `unit_price`, `unit`, `stock_quantity`, `warning_threshold`, `spec_code`) VALUES
-- 不锈钢合页 (product_id=1)
(1, '304不锈钢', '62mm×50mm',  8.50,  '个', 200, 20, 'HY001-62'),
(1, '304不锈钢', '75mm×60mm',  12.00, '个', 150, 20, 'HY001-75'),
(1, '304不锈钢', '100mm×75mm', 18.00, '个', 100, 10, 'HY001-100'),
-- 球形门锁 (product_id=2)
(2, '锌合金', '室内款（标准圆舌）',    38.00, '个',  80, 10, 'MS002-A'),
(2, '锌合金', '加强款（斜舌+圆舌）',  62.00, '个',  60, 10, 'MS002-B'),
-- 铝合金推拉窗滑轮 (product_id=3)
(3, '铝合金+不锈钢轴承', '小号（适配20mm槽）', 5.50,  '对', 120, 15, 'HL003-S'),
(3, '铝合金+不锈钢轴承', '大号（适配25mm槽）', 8.00,  '对',  80, 10, 'HL003-L'),
-- 单冷厨房水龙头 (product_id=4)
(4, '304不锈钢', '单冷款（弯管出水）', 49.00,  '个',  50, 5, 'SK004-C'),
(4, '304不锈钢', '冷热款（混水阀）',   98.00,  '个',  40, 5, 'SK004-W'),
-- PPR管道热熔接头 (product_id=5)
(5, 'PPR', 'DN20（外径20mm）', 1.20,  '个', 500, 50, 'PPR005-20'),
(5, 'PPR', 'DN25（外径25mm）', 1.80,  '个', 400, 50, 'PPR005-25'),
(5, 'PPR', 'DN32（外径32mm）', 2.50,  '个', 300, 30, 'PPR005-32'),
-- 全铜球阀 (product_id=6)
(6, '全铜（铅黄铜）', 'DN15（4分）', 12.00, '个', 100, 10, 'QF006-15'),
(6, '全铜（铅黄铜）', 'DN20（6分）', 18.00, '个',  80, 10, 'QF006-20'),
(6, '全铜（铅黄铜）', 'DN25（1寸）', 28.00, '个',  60,  5, 'QF006-25'),
-- 不锈钢膨胀螺丝 (product_id=7)
(7, '304不锈钢', 'M6×60mm（50个/包）',  8.00,  '包', 200, 20, 'PZ007-M6'),
(7, '304不锈钢', 'M8×80mm（50个/包）',  12.00, '包', 150, 15, 'PZ007-M8'),
(7, '304不锈钢', 'M10×100mm（50个/包）',18.00, '包', 100, 10, 'PZ007-M10'),
-- 活动扳手 (product_id=8)
(8, '铬钒钢（锻造）', '6寸（开口25mm）',  22.00, '个',  80, 10, 'BH008-6'),
(8, '铬钒钢（锻造）', '8寸（开口30mm）',  35.00, '个',  60,  8, 'BH008-8'),
(8, '铬钒钢（锻造）', '12寸（开口36mm）', 58.00, '个',  40,  5, 'BH008-12'),
-- 螺丝刀套装 (product_id=9)
(9, '铬钒钢', '8件套（十字PH0-PH3+一字SL3-6mm）', 45.00, '套', 60, 8, 'SD009-8'),
(9, '铬钒钢', '12件套（含特殊头型）',               72.00, '套', 40, 5, 'SD009-12'),
-- 冲击电钻 (product_id=10)
(10, '工程塑料+铬钢', '单机版（含13mm自紧夹头）',   199.00, '个', 20, 3, 'CD010-S'),
(10, '工程塑料+铬钢', '套装版（含钻头19件+工具箱）', 269.00, '套', 15, 2, 'CD010-P'),
-- 钢卷尺 (product_id=11)
(11, '碳钢尺带+ABS外壳', '3米×16mm',  18.00, '个', 100, 10, 'JC011-3'),
(11, '碳钢尺带+ABS外壳', '5米×25mm',  26.00, '个',  80, 10, 'JC011-5'),
(11, '碳钢尺带+ABS外壳', '7.5米×25mm',45.00, '个',  50,  5, 'JC011-75'),
-- 不锈钢厨房水槽 (product_id=12)
(12, '304不锈钢（拉丝）', '单槽500×400mm', 328.00, '个', 15, 2, 'SC012-5040'),
(12, '304不锈钢（拉丝）', '单槽600×450mm', 498.00, '个', 10, 2, 'SC012-6045'),
(12, '304不锈钢（拉丝）', '双槽800×450mm', 698.00, '个',  8, 2, 'SC012-8045'),
-- 不锈钢毛巾架 (product_id=13)
(13, '304不锈钢', '单杆450mm', 88.00,  '个', 30, 5, 'MJ013-45'),
(13, '304不锈钢', '双杆600mm', 135.00, '个', 20, 3, 'MJ013-60');

-- ===================== 商品主图初始数据 =====================
-- 使用 static/images/hardware/ 目录下已有图片，image_sort=1 表示主图
INSERT INTO `product_image` (`product_id`, `image_url`, `image_sort`, `is_deleted`) VALUES
(1,  '/images/hardware/001.png', 1, 0),
(2,  '/images/hardware/002.png', 1, 0),
(3,  '/images/hardware/003.png', 1, 0),
(4,  '/images/hardware/004.png', 1, 0),
(5,  '/images/hardware/005.png', 1, 0),
(6,  '/images/hardware/006.png', 1, 0),
(7,  '/images/hardware/007.png', 1, 0),
(8,  '/images/hardware/008.png', 1, 0),
(9,  '/images/hardware/009.png', 1, 0),
(10, '/images/hardware/010.png', 1, 0),
(11, '/images/hardware/011.png', 1, 0),
(12, '/images/hardware/012.png', 1, 0),
(13, '/images/hardware/013.png', 1, 0);
