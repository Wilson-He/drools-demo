package io.github.test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.test.constant.ProductConstant;
import io.github.test.model.Customer;
import io.github.test.model.Product;
import org.apache.camel.component.quartz.QuartzHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.drools.core.base.RuleNameEqualsAgendaFilter;
import org.kie.api.KieServices;
import org.kie.api.cdi.KSession;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.conf.TimedRuleExecutionFilter;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.rule.FactHandle;
import org.quartz.impl.QuartzServer;
import org.quartz.impl.calendar.DailyCalendar;
import org.quartz.impl.calendar.WeeklyCalendar;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.*;

/**
 * @author Wilson
 * @date 2019/3/13
 */

public class MainTest {

    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("demo-pool-%d").build();
    private static ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());


    public static void main(String[] args) throws Exception {
//        agendaGroup();
//        update();
//        fire();
//        noLoopAndLockOnActive();
//        dateEffective();
//        timerDrl();
//        haltDrl();
//        modifyDrl();
        forallOrExists();
    }

    public static void forallOrExists() {
        KieSession kieSession = createSession("forall-or-exists");
        Product product = new Product().setType(ProductConstant.Type.GLOD)
                .setPrice(10D);
        kieSession.insert(product);
        kieSession.insert(new Customer().setAge(18));
        kieSession.fireAllRules();
    }


    public static void modifyDrl() {
        KieSession kieSession = createSession("modifyExpr");
        Product product = new Product().setType(ProductConstant.Type.DIAMOND);
        kieSession.insert(product);
        kieSession.fireAllRules();
        System.out.println(product);
    }

    public static void timerDrl() throws InterruptedException, ParseException {
       /* KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
//        配置KieSession以自动执行定时规则
        ksconf.setOption(TimedRuleExecutionOption.YES);
//         只执行ruleName=once-timer-delay的规则
        TimedRuleExecutionOption.FILTERED ruleFilter = new TimedRuleExecutionOption.FILTERED(
                rules -> Arrays.stream(rules)
                        .anyMatch(rule -> StringUtils.equals(rule.getName(), "interval-timer-delay")));
//        ksconf.setOption(ruleFilter);
        KieSession kieSession = KieServices.get().getKieClasspathContainer().newKieSession("timer", ksconf);*/
        WeeklyCalendar weekDayCal = new WeeklyCalendar();
        // 下标分别对应是否排除周末、周日、周六...周一 isDayExcluded检测是否排除java.util.Calendar.DAY_OF_WEEK ... MONDAY
        // 将对应的天周X下标设为true则到到当天将不执行规则
        weekDayCal.setDaysExcluded(new boolean[]{false, false, false, false, false, false, false, false, false});
        KieSession kieSession = KieServices.get().getKieClasspathContainer().newKieSession("timer");
        kieSession.getCalendars().set("calendarRange", weekDayCal::isTimeIncluded);
        System.err.println("start timer");
        kieSession.getCalendars().set("nowDay", timestamp -> DateUtils.isSameDay(new Date(timestamp), new Date()));
        singleThreadPool.execute(() -> kieSession.fireUntilHalt(new RuleNameEqualsAgendaFilter("interval-timer-delay")));
        Thread.sleep(7000);
        kieSession.halt();
        singleThreadPool.shutdown();
        // 释放session资源
        kieSession.dispose();
    }

    /**
     * fireAllRules与fireUntilHalt作用比对
     *
     * @throws InterruptedException
     */
    public static void haltDrl() throws InterruptedException {
        KieSession kSession = createSession("halt");
        Product product = new Product().setPrice(3D);
        FactHandle factHandle = kSession.insert(product);
        //singleThreadPool.execute(kSession::fireAllRules);
        // fireUntilHalt会在被halt之前一直检测factHandle的变化然后重新匹配规则
        singleThreadPool.execute(kSession::fireUntilHalt);
        singleThreadPool.shutdown();
        Thread.sleep(2000);
        product.setPrice(10D);
        kSession.update(factHandle, product);
        Thread.sleep(2000);
        kSession.halt();
        kSession.dispose();
    }


    public static void dateEffectiveDrl() {
        KieSession kSession = createSession("date-effective");
        kSession.fireAllRules();
        kSession.dispose();
    }

    public static void noloopDrl() {
        KieSession kSession = createSession("no-loop");
        Product product = new Product().setPrice(1D);
        FactHandle factHandle = kSession.insert(product);
        kSession.fireAllRules();
        kSession.dispose();
    }

    public static void fire() {
        // 获取kmodule.xml中配置中名称为ksession-rule的session，默认为有状态的。
        KieSession kSession = createSession(null);
        Product product = new Product();
        product.setType(ProductConstant.Type.GLOD);
        product.setPercent(50);
        product.setPrice(80D);
        // 触发执行规则
        kSession.insert(product);
        System.out.println("运算前：" + product);
        int count = kSession.fireAllRules();
        System.out.println("命中了" + count + "条规则！");
        System.out.println("商品" + product.getType() + "的商品折扣为" + product.getDiscount() + "%,价格:" + product.getPrice());
        System.out.println("运算后：" + product);
        kSession.dispose();
    }

    public static void agendaGroupDrl() {
        KieSession kSession = createSession(null);
        kSession.getAgenda().getAgendaGroup("agenda-group").setFocus();
        kSession.fireAllRules();
        kSession.dispose();
    }

    public static void update() {
        KieSession kSession = createSession("loop-rule");
        Product product = new Product();
        product.setDiscount(2D);
        kSession.insert(product);
        int count = kSession.fireAllRules();
        System.out.println("命中了" + count + "条规则！");
        System.out.println("商品折扣为" + product.getDiscount() + "%。");
        kSession.dispose();
    }

    private static KieSession createSession(String sessionName) {
        return KieServices.Factory.get().getKieClasspathContainer().newKieSession(sessionName);
    }

    private static StatelessKieSession createStatelessSession(String sessionName) {
        return KieServices.Factory.get().getKieClasspathContainer().newStatelessKieSession(sessionName);
    }

}
