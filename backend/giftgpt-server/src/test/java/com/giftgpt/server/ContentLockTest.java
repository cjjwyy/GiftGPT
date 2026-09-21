package com.giftgpt.server;

import com.giftgpt.content.mapper.StoryMapper;
import com.giftgpt.order.mapper.PackagingMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated in-memory H2: verifies mapper locking SQL and concurrent count writes. */
class ContentLockTest {
    @Test void databaseLocksSerializeCountersAndOwnerLockIsValid() throws Exception {
        String url="jdbc:h2:mem:content-lock;MODE=MySQL;NON_KEYWORDS=USER;LOCK_TIMEOUT=3000";
        String lock=StoryMapper.class.getMethod("lockById",Long.class).getAnnotation(Select.class).value()[0].replace("#{id}","1");
        String owner=PackagingMapper.class.getMethod("lockOwner",Long.class).getAnnotation(Select.class).value()[0].replace("#{id}","1");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try (Connection setup=DriverManager.getConnection(url); Statement st=setup.createStatement()) {
            st.execute("CREATE TABLE user(id BIGINT PRIMARY KEY)");st.execute("INSERT INTO user VALUES(1)");
            st.execute("CREATE TABLE story(id BIGINT PRIMARY KEY, likes INT)");st.execute("INSERT INTO story VALUES(1,0)");
            try (ResultSet rs=st.executeQuery(owner)) { assertTrue(rs.next()); }
            CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
            Callable<Void> increment=()->{
                try(Connection c=DriverManager.getConnection(url)) {
                    c.setAutoCommit(false);ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));
                    try(Statement s=c.createStatement()) {
                        int count;
                        try(ResultSet rs=s.executeQuery(lock)) { rs.next(); count=rs.getInt("likes"); }
                        s.executeUpdate("UPDATE story SET likes="+(count+1)+" WHERE id=1");
                        c.commit();
                    }
                }
                return null;
            };
            Future<Void> a=pool.submit(increment), b=pool.submit(increment);
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            a.get(5,TimeUnit.SECONDS);b.get(5,TimeUnit.SECONDS);
            try(ResultSet rs=st.executeQuery("SELECT likes FROM story WHERE id=1")) {rs.next();assertEquals(2,rs.getInt(1));}
        } finally { pool.shutdownNow(); }
    }
}
