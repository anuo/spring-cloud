package com.fangjia.sjdbc.controller;

import com.fangjia.sjdbc.po.DataSourceInfo;
import com.fangjia.sjdbc.po.User;
import com.fangjia.sjdbc.service.UserService;
import com.fangjia.sjdbc.util.ZookeeperUtil;
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

        DataSourceInfo currentDsInfo = new DataSourceInfo();
        currentDsInfo.setIp("192.168.10.48");
        currentDsInfo.setPort("192.168.10.48");


        return "success";
//
//        Map<String, DataSource> result = DataSourceConverter.dataSourceMapFromYaml(getDirectly(path, client));
//        DataSource dataSource = result.get("jk");
//
////        String zkstring= MasterSlaveConfigurationConverter.configMapToYaml(result);
//        return result;

    }






}
