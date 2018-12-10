package com.fangjia.sjdbc.util;

import com.fangjia.sjdbc.po.DataSourceInfo;
import com.google.common.base.Charsets;
import io.shardingsphere.orchestration.reg.zookeeper.curator.CuratorZookeeperExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.text.MessageFormat;

public class DataSourceUtil {

    public static void switchDataSource(DataSourceInfo currentDataSourceInfo, DataSourceInfo newDataSourceInfo) {

        //todo 参数检查
        //连接zookeeper
        CuratorFramework client = ZookeeperUtil.createClient("192.168.10.48:2181");
        client.getUnhandledErrorListenable().addListener((message, e) -> {
            System.err.println("error=" + message);
            e.printStackTrace();
        });
        client.getConnectionStateListenable().addListener((c, newState) -> {
            System.out.println("state=" + newState);
        });

        client.start();

        //获取当前数据源信息
        String path = "/sharding-jdbc-orchestration/orchestration-sharding-data-source/config/datasource";
        String currentDSInfoStr = ZookeeperUtil.getDirectly(path, client);

        //替换 dbName
        currentDSInfoStr = currentDSInfoStr.replace("/"+currentDataSourceInfo.getDbName()+"?"
                , "/"+newDataSourceInfo.getDbName()+"?");

        //替换 账号
        currentDSInfoStr = currentDSInfoStr.replace("username: "+currentDataSourceInfo.getUserName()
                , "username: "+newDataSourceInfo.getUserName());

        //替换 密码
        currentDSInfoStr = currentDSInfoStr.replace("password: "+currentDataSourceInfo.getPwd()
                , "password: "+newDataSourceInfo.getPwd());

        //落zk 更新配置
        ZookeeperUtil.persist(path, currentDSInfoStr, client);
    }

//    private String getUrl(DataSourceInfo dataSourceInfo) {
//        return MessageFormat.format("jdbc:mysql://{0}:{1}/{2}?serverTimezone=Asia/Shanghai&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false"
//                , dataSourceInfo.getIp(), dataSourceInfo.getPort(), dataSourceInfo.getDbName());
//    }
//



}
