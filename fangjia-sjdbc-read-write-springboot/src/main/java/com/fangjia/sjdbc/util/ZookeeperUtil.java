package com.fangjia.sjdbc.util;

import com.google.common.base.Charsets;
import io.shardingsphere.orchestration.reg.zookeeper.curator.CuratorZookeeperExceptionHandler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

public class ZookeeperUtil {

    public static CuratorFramework createClient(String connectionString) {
        // these are reasonable arguments for the ExponentialBackoffRetry. The first
        // retry will wait 1 second - the second will wait up to 2 seconds - the
        // third will wait up to 4 seconds.
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // The simplest way to get a CuratorFramework instance. This will use default values.
        // The only required arguments are the connection string and the retry policy
        return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
    }

    public static String getDirectly(final String key, CuratorFramework client) {
        try {
            return new String(client.getData().forPath(key), Charsets.UTF_8);

            //CHECKSTYLE:OFF
        } catch (final Exception ex) {

            //CHECKSTYLE:ON
            return null;
        }
    }

    public static void persist(final String key, final String value, CuratorFramework client) {
        try {
            if (!isExisted(key, client)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(key, value.getBytes(Charsets.UTF_8));
            } else {

                update(key, value, client);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            CuratorZookeeperExceptionHandler.handleException(ex);
        }
    }

    public static void update(final String key, final String value, CuratorFramework client) {
        try {
            client.inTransaction().check().forPath(key).and().setData().forPath(key, value.getBytes(Charsets.UTF_8)).and().commit();
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            CuratorZookeeperExceptionHandler.handleException(ex);
        }
    }

    public static boolean isExisted(final String key, CuratorFramework client) {
        try {
            return null != client.checkExists().forPath(key);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            CuratorZookeeperExceptionHandler.handleException(ex);
            return false;
        }
    }
}
