package com.florachain;

import com.florachain.common.BusinessException;
import com.florachain.common.Result;
import com.florachain.delivery.AreaDispatchStrategy;
import com.florachain.delivery.Courier;
import com.florachain.delivery.DeliveryContext;
import com.florachain.delivery.DistanceFeeRule;
import com.florachain.delivery.WeightFeeRule;
import com.florachain.infra.InMemoryEventBus;
import com.florachain.infra.TokenBucketRateLimiter;
import com.florachain.inventory.BatchAllocator;
import com.florachain.inventory.DaysBeforeExpiryPolicy;
import com.florachain.inventory.DeductResult;
import com.florachain.inventory.ExpiryLevel;
import com.florachain.inventory.ExpiryWarningService;
import com.florachain.inventory.FlowerBatch;
import com.florachain.inventory.InMemoryStockCache;
import com.florachain.inventory.OptimisticLockStrategy;
import com.florachain.inventory.RedisPreReserveStrategy;
import com.florachain.order.FlashSaleService;
import com.florachain.order.InMemoryOrderRepository;
import com.florachain.order.Order;
import com.florachain.order.OrderStateMachine;
import com.florachain.order.OrderStatus;
import com.florachain.order.OrderTimeoutService;
import com.florachain.order.PaymentCallbackHandler;
import com.florachain.payment.AlipayChannel;
import com.florachain.payment.CallbackResult;
import com.florachain.payment.InMemoryIdempotencyStore;
import com.florachain.payment.WechatPayChannel;
import com.florachain.promotion.CouponPromotion;
import com.florachain.promotion.FlashSalePromotion;
import com.florachain.promotion.FullReductionPromotion;
import com.florachain.promotion.PromotionContext;
import com.florachain.promotion.PromotionEngine;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** 无第三方依赖的核心框架验证程序，运行后逐项打印通过或失败。 */
public class CoreVerification {
    private static int pass, fail;

    private static void check(String name, boolean ok) {
        if (ok) pass++; else fail++;
        System.out.println((ok ? "[通过] " : "[失败] ") + name);
    }

    public static void main(String[] args) throws Exception {
        stateMachine();
        batchAndExpiry();
        promotionAndDelivery();
        paymentCallback();
        timeoutRelease();
        concurrentPreReserve();
        concurrentOptimisticLock();
        rateLimit();
        System.out.println("\n结果：通过 " + pass + " 项，失败 " + fail + " 项");
        if (fail > 0) System.exit(1);
    }

    static void stateMachine() {
        check("状态机：待支付→已支付 合法", OrderStateMachine.canTransit(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID));
        check("状态机：售后中→已完成 合法（BUG-011）", OrderStateMachine.canTransit(OrderStatus.AFTER_SALE, OrderStatus.COMPLETED));
        check("状态机：已取消→已支付 非法", !OrderStateMachine.canTransit(OrderStatus.CANCELLED, OrderStatus.PAID));
        boolean thrown = false;
        try {
            OrderStateMachine.assertTransit(OrderStatus.COMPLETED, OrderStatus.PREPARING);
        } catch (BusinessException e) {
            thrown = e.getCode() == 4001;
        }
        check("状态机：非法流转抛出业务异常(4001)", thrown);
    }

    static void batchAndExpiry() {
        LocalDate today = LocalDate.of(2026, 11, 12);
        List<FlowerBatch> batches = List.of(
                new FlowerBatch("B3", "rose", "S1", today.minusDays(1), 7, 30),   // 11-18 到期
                new FlowerBatch("B1", "rose", "S1", today.minusDays(5), 7, 20),   // 11-14 到期，最早
                new FlowerBatch("B2", "rose", "S2", today.minusDays(3), 7, 10),   // 11-16 到期
                new FlowerBatch("B0", "rose", "S2", today.minusDays(9), 7, 50));  // 已过期
        var alloc = new BatchAllocator().allocate(batches, 35, today);
        check("出库先进先出：先用最早到期的批次B1，再B2，再B3",
                alloc.size() == 3 && alloc.get(0).batchId().equals("B1") && alloc.get(0).quantity() == 20
                        && alloc.get(1).batchId().equals("B2") && alloc.get(2).batchId().equals("B3")
                        && alloc.get(2).quantity() == 5);
        boolean noExpired = alloc.stream().noneMatch(a -> a.batchId().equals("B0"));
        check("出库分配不使用已过期批次", noExpired);
        boolean insufficient = false;
        try {
            new BatchAllocator().allocate(batches, 100, today);
        } catch (BusinessException e) {
            insufficient = e.getCode() == 4100;
        }
        check("可用库存不足时抛出业务异常(4100)", insufficient);
        var svc = new ExpiryWarningService(Map.of(), new DaysBeforeExpiryPolicy(3, 1));
        var warnings = svc.scan(batches, today);
        Map<String, ExpiryLevel> byId = new HashMap<>();
        warnings.forEach(w -> byId.put(w.batch().batchId(), w.level()));
        check("保鲜期预警：B0已过期，B1剩2天进入预警，B3剩6天不预警",
                byId.get("B0") == ExpiryLevel.EXPIRED && byId.get("B1") == ExpiryLevel.WARNING && !byId.containsKey("B3"));
    }

    static void promotionAndDelivery() {
        var engine = new PromotionEngine(List.of(
                new FullReductionPromotion(new BigDecimal("100"), new BigDecimal("15")),
                new CouponPromotion("NEWUSER", new BigDecimal("20")),
                new FlashSalePromotion(new BigDecimal("0.8"))));
        var best = engine.best(new PromotionContext(new BigDecimal("200"), "NEWUSER", true)).orElseThrow();
        check("营销：同时满足满减、优惠券、限时折扣时选优惠最大的（限时折扣减40）",
                best.name().equals("限时折扣") && best.discount().compareTo(new BigDecimal("40")) == 0);
        check("营销：金额不足且无券无活动时无优惠",
                engine.best(new PromotionContext(new BigDecimal("50"), null, false)).isEmpty());
        var dist = new DistanceFeeRule(new BigDecimal("6"), new BigDecimal("199"), 3, new BigDecimal("2"));
        var weight = new WeightFeeRule(new BigDecimal("6"), new BigDecimal("199"), 5, new BigDecimal("1.5"));
        var ctx = new DeliveryContext(7.2, 8.0, new BigDecimal("120"), "A区");
        check("运费：距离规则 6+ceil(4.2)*2=16", dist.fee(ctx).compareTo(new BigDecimal("16")) == 0);
        check("运费：重量规则 6+3*1.5=10.5", weight.fee(ctx).compareTo(new BigDecimal("10.5")) == 0);
        check("运费：满199元包邮", dist.fee(new DeliveryContext(7.2, 8.0, new BigDecimal("200"), "A区")).signum() == 0);
        var couriers = List.of(new Courier("c1", "A区", 5), new Courier("c2", "A区", 2), new Courier("c3", "B区", 0));
        check("派单：同区域内选任务最少的配送员c2",
                new AreaDispatchStrategy().assign("A区", couriers).orElseThrow().id().equals("c2"));
        check("派单：无该区域配送员时返回空", new AreaDispatchStrategy().assign("C区", couriers).isEmpty());
    }

    static void paymentCallback() {
        var repo = new InMemoryOrderRepository();
        var order = new Order("FC-PAY-1", 1L, "rose-box", 1, new BigDecimal("99.00"), "2026-11-14 10:00-12:00", LocalDateTime.now());
        repo.save(order);
        var handler = new PaymentCallbackHandler(repo);
        var idem = new InMemoryIdempotencyStore();
        var wx = new WechatPayChannel("wx-secret", idem, handler);
        var ali = new AlipayChannel("ali-secret", idem, handler);

        Map<String, String> p = new HashMap<>();
        p.put("out_trade_no", "FC-PAY-1");
        p.put("transaction_id", "WX-TXN-1");
        p.put("result_code", "SUCCESS");
        p.put("total_fee", "9900");
        p.put("sign", wx.sign(p));
        check("支付回调：第一次回调处理成功，订单变为已支付",
                wx.handleCallback(p).type() == CallbackResult.Type.ACCEPTED && order.status() == OrderStatus.PAID);
        check("支付回调：重复回调被识别并忽略（BUG-007 幂等）",
                wx.handleCallback(p).type() == CallbackResult.Type.DUPLICATE && order.logs().size() == 1);
        Map<String, String> forged = new HashMap<>(p);
        forged.put("transaction_id", "WX-TXN-2");   // 篡改后签名不再匹配
        check("支付回调：被篡改的回调验签失败",
                wx.handleCallback(forged).type() == CallbackResult.Type.REJECTED);
        Map<String, String> a = new HashMap<>();
        a.put("out_trade_no", "FC-PAY-1");
        a.put("trade_no", "ALI-TXN-1");
        a.put("trade_status", "WAIT_BUYER_PAY");
        a.put("total_amount", "99.00");
        a.put("sign", ali.sign(a));
        check("支付回调：支付宝非成功状态被忽略", ali.handleCallback(a).type() == CallbackResult.Type.IGNORED);
    }

    static void timeoutRelease() {
        var cache = new InMemoryStockCache();
        var stock = new RedisPreReserveStrategy(cache);
        stock.initStock("rose-box", 10);
        var repo = new InMemoryOrderRepository();
        var bus = new InMemoryEventBus();
        var flash = new FlashSaleService(new TokenBucketRateLimiter(1000, 1000), stock, repo, bus);
        var r = flash.buy(7L, "rose-box", 3, new BigDecimal("50"), "2026-11-14 10:00-12:00");
        check("下单：预扣库存成功，剩余7", r.isOk() && stock.remaining("rose-box") == 7);
        var timeout = new OrderTimeoutService(repo, stock);
        int closed = timeout.closeTimeoutOrders(LocalDateTime.now().plusMinutes(31), Duration.ofMinutes(30));
        check("超时关闭：31分钟未支付的订单被关闭并释放库存回到10", closed == 1 && stock.remaining("rose-box") == 10);
        check("超时关闭：重复执行不会重复释放", timeout.closeTimeoutOrders(LocalDateTime.now().plusMinutes(40), Duration.ofMinutes(30)) == 0
                && stock.remaining("rose-box") == 10);
        check("幂等：同一订单号重复预扣返回重复",
                stock.deduct("rose-box", 1, "DUP-1") == DeductResult.SUCCESS && stock.deduct("rose-box", 1, "DUP-1") == DeductResult.DUPLICATE
                        && stock.remaining("rose-box") == 9);
        try { bus.drainAndClose(); } catch (InterruptedException ignored) { }
    }

    /** 10000个用户同时抢500件：预扣方案不超卖，且恰好卖完。 */
    static void concurrentPreReserve() throws Exception {
        final int stockQty = 500, buyers = 10_000;
        var cache = new InMemoryStockCache();
        var stock = new RedisPreReserveStrategy(cache);
        stock.initStock("hot-sku", stockQty);
        var repo = new InMemoryOrderRepository();
        var bus = new InMemoryEventBus();
        AtomicInteger consumed = new AtomicInteger();
        bus.subscribe(e -> consumed.incrementAndGet());
        var flash = new FlashSaleService(new TokenBucketRateLimiter(1e9, 1e9), stock, repo, bus);

        AtomicInteger ok = new AtomicInteger(), soldOut = new AtomicInteger();
        runConcurrently(buyers, i -> {
            Result<String> r = flash.buy(i, "hot-sku", 1, new BigDecimal("99"), "2026-11-14 10:00-12:00");
            if (r.isOk()) ok.incrementAndGet(); else if (r.code() == 410) soldOut.incrementAndGet();
        });
        bus.drainAndClose();
        System.out.println("      预扣方案：成功 " + ok + "，已抢完 " + soldOut + "，剩余库存 " + stock.remaining("hot-sku"));
        check("并发抢购(预扣)：10000人抢500件，恰好成功500单，不超卖", ok.get() == stockQty && soldOut.get() == buyers - stockQty);
        check("并发抢购(预扣)：库存为0，订单数500，异步事件消费500条",
                stock.remaining("hot-sku") == 0 && repo.count() == stockQty && consumed.get() == stockQty);
    }

    /** 对照方案：内存中的乐观锁同样不会超卖。真实数据库每次更新要经过网络往返，冲突窗口更大，需要用压测验证。 */
    static void concurrentOptimisticLock() throws Exception {
        final int stockQty = 500, buyers = 10_000;
        var stock = new OptimisticLockStrategy(3);
        stock.initStock("hot-sku", stockQty);
        AtomicInteger ok = new AtomicInteger(), conflict = new AtomicInteger();
        runConcurrently(buyers, i -> {
            try {
                if (stock.deduct("hot-sku", 1, "O" + i) == DeductResult.SUCCESS) ok.incrementAndGet();
            } catch (BusinessException e) {
                conflict.incrementAndGet();
            }
        });
        System.out.println("      乐观锁方案(重试3次)：成功 " + ok + "，因版本冲突失败 " + conflict + "，剩余库存 " + stock.remaining("hot-sku"));
        check("并发抢购(乐观锁对照)：不超卖且成功数不超过库存",
                ok.get() <= stockQty && stock.remaining("hot-sku") == stockQty - ok.get() && stock.remaining("hot-sku") >= 0);
    }

    static void rateLimit() {
        var limiter = new TokenBucketRateLimiter(100, 0.001);
        int passed = 0;
        for (int i = 0; i < 1000; i++) if (limiter.tryAcquire()) passed++;
        check("限流：令牌桶容量100时，瞬时1000个请求只放行约100个（实际放行" + passed + "）", passed >= 100 && passed <= 101);
    }

    static void runConcurrently(int tasks, java.util.function.IntConsumer body) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(200);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            final int id = i;
            pool.submit(() -> {
                try { start.await(); body.accept(id); } catch (InterruptedException ignored) { } finally { done.countDown(); }
            });
        }
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();
    }
}
