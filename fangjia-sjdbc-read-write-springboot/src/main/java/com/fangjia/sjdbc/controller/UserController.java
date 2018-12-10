package com.fangjia.sjdbc.controller;

import com.fangjia.sjdbc.po.DataSourceInfo;
import com.fangjia.sjdbc.po.User;
import com.fangjia.sjdbc.service.UserService;
import com.fangjia.sjdbc.util.DataSourceUtil;
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

    @GetMapping("/findGpsList")
    public Object findGpsList() {
        return userService.findGpsList();
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

        if (oldDbName.equals("car_manage")) {
            currentDsInfo = getCarManage();
        } else {
            currentDsInfo.setDbName(oldDbName);
            currentDsInfo.setUserName("root");
            currentDsInfo.setPwd("MTIzNDU2");

        }

        DataSourceInfo newDsInfo = new DataSourceInfo();
        newDsInfo.setDbName(newDbName);
        newDsInfo.setUserName("root");
        newDsInfo.setPwd("MTIzNDU2");

        DataSourceUtil.switchDataSource(currentDsInfo, newDsInfo);

        return "200";
    }

    private DataSourceInfo getCarManage() {
        DataSourceInfo currentDsInfo = new DataSourceInfo();
        currentDsInfo.setDbName("car_manage");
        currentDsInfo.setUserName("root");
        currentDsInfo.setPwd("root");
        return currentDsInfo;
    }


}
