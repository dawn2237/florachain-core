# 鲜花云链 核心框架代码（florachain-core）

软件项目管理综合实验：鲜花云链——线上鲜花商城订单与供应链管理系统。本工程是"框架设计、架构设计和核心代码"部分，
只包含领域核心层（抽象类、接口、子类）和一个无第三方依赖的验证程序，不依赖 Spring、Redis、RabbitMQ 即可编译运行。
生产环境中，接口由 Spring Boot 工程里的 MySQL、Redis、RabbitMQ 适配类实现；这里的 InMemory* 类用于在本地验证核心逻辑。

## 环境
JDK 17 及以上（开发时使用 JDK 21 验证）。

## 编译与运行
```bash
# 在工程根目录执行
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out com.florachain.CoreVerification
```
Windows PowerShell 可使用：
```powershell
mkdir out
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out com.florachain.CoreVerification
```
运行后逐项打印"通过/失败"，全部通过时输出"通过 27 项，失败 0 项"。

## 包结构
| 包 | 职责 | 关键类型 |
|---|---|---|
| common | 统一响应与业务异常 | Result、BusinessException |
| order | 订单、状态机、抢购下单、超时关闭、支付后处理 | Order、OrderStateMachine、FlashSaleService、OrderTimeoutService |
| inventory | 库存扣减、批次出库、保鲜期预警 | StockDeductionStrategy（接口）、AbstractStockDeductionStrategy（抽象类）、RedisPreReserveStrategy、OptimisticLockStrategy、BatchAllocator、ExpiryPolicy |
| payment | 支付渠道与回调 | PaymentChannel（接口）、AbstractPaymentChannel（抽象类）、WechatPayChannel、AlipayChannel |
| promotion | 营销活动 | PromotionStrategy（接口）、FullReductionPromotion、CouponPromotion、FlashSalePromotion |
| delivery | 运费与派单 | DeliveryFeeRule（抽象类）、DistanceFeeRule、WeightFeeRule、DispatchStrategy |
| infra | 限流与事件 | RateLimiter、TokenBucketRateLimiter、EventPublisher、InMemoryEventBus |

## 需求与代码的对应
| 需求 | 代码 |
|---|---|
| FR-O01、FR-O08 下单与限量抢购 | FlashSaleService |
| FR-O02 订单状态流转 | OrderStatus、OrderStateMachine、Order |
| FR-O03、FR-O04 支付与幂等回调 | PaymentChannel、AbstractPaymentChannel、PaymentCallbackHandler |
| FR-O06 超时关闭 | OrderTimeoutService |
| FR-S04 库存扣减、防超卖 | StockDeductionStrategy 及其实现、BatchAllocator |
| FR-S05 保鲜期预警 | ExpiryPolicy、ExpiryWarningService |
| FR-U08 营销活动 | PromotionStrategy 及其实现、PromotionEngine |
| FR-D01 运费规则 | DeliveryFeeRule 及其实现 |
| FR-D02 派单 | DispatchStrategy、AreaDispatchStrategy |
