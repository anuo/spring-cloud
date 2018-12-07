package com.fangjia.sjdbc.controller;

import com.fangjia.sjdbc.po.User;
import com.fangjia.sjdbc.service.UserService;
import com.google.common.base.Charsets;
import io.shardingsphere.orchestration.reg.zookeeper.curator.CuratorZookeeperExceptionHandler;
import io.shardingsphere.orchestration.reg.zookeeper.curator.CuratorZookeeperRegistryCenter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public Object list() {
        return userService.list();
    }

    @GetMapping("/add")
    public Object add() {
        User user = new User();
        user.setId(100L);
        user.setCity("深圳");
        user.setName("李四");
        return userService.add(user);
    }

    @GetMapping("/changedb/{oldDbName}/to/{newDbName}")
    public Object changedb(@PathVariable("oldDbName") String oldDbName, @PathVariable("newDbName") String newDbName) throws Exception {

        CuratorFramework client = createClient("192.168.10.48:2181");
        client.getUnhandledErrorListenable().addListener((message, e) -> {
            System.err.println("error=" + message);
            e.printStackTrace();
        });
        client.getConnectionStateListenable().addListener((c, newState) -> {
            System.out.println("state=" + newState);
        });

        client.start();

        String path = "/sharding-jdbc-orchestration/orchestration-sharding-data-source/config/datasource";
        String zkS = getDirectly(path, client);
        zkS = zkS.replace("/" + oldDbName + "?", "/" + newDbName + "?");

        persist(path, zkS, client);
        return "success";
//
//        Map<String, DataSource> result = DataSourceConverter.dataSourceMapFromYaml(getDirectly(path, client));
//        DataSource dataSource = result.get("jk");
//
////        String zkstring= MasterSlaveConfigurationConverter.configMapToYaml(result);
//        return result;

    }


    public static List<String> watchedGetChildren(CuratorFramework client, String path) throws Exception {
        /**
         * Get children and set a watcher on the node. The watcher notification will come through the
         * CuratorListener (see setDataAsync() above).
         */
        return client.getChildren().watched().forPath(path);
    }


    public static CuratorFramework createClient(String connectionString) {
        // these are reasonable arguments for the ExponentialBackoffRetry. The first
        // retry will wait 1 second - the second will wait up to 2 seconds - the
        // third will wait up to 4 seconds.
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // The simplest way to get a CuratorFramework instance. This will use default values.
        // The only required arguments are the connection string and the retry policy
        return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
    }

    public String getDirectly(final String key, CuratorFramework client) {
        try {
            return new String(client.getData().forPath(key), Charsets.UTF_8);

            //CHECKSTYLE:OFF
        } catch (final Exception ex) {

            //CHECKSTYLE:ON
            return null;
        }
    }

    public void persist(final String key, final String value, CuratorFramework client) {
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

    public void update(final String key, final String value, CuratorFramework client) {
        try {
            client.inTransaction().check().forPath(key).and().setData().forPath(key, value.getBytes(Charsets.UTF_8)).and().commit();
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            CuratorZookeeperExceptionHandler.handleException(ex);
        }
    }

    public boolean isExisted(final String key, CuratorFramework client) {
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
