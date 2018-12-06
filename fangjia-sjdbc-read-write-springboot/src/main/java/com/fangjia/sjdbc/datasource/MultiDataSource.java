package com.fangjia.sjdbc.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 多数据源实现
 */
public class MultiDataSource extends AbstractRoutingDataSource {
    private static String singleDataSourceKey;

    private MultiDataSource() {
    }

    private static MultiDataSource multiDataSource;

    public static MultiDataSource getInstance() {
        if (multiDataSource == null) {
            multiDataSource = new MultiDataSource();
        }
        return multiDataSource;
    }

    private Map<Object, Object> dynamicTargetDataSources = new HashMap<>();
    private Set<String> datasourceKeys = new HashSet<>();

//    @Value("${datasource.initialSize}")
//    private int initialSize;
//    @Value("${datasource.minIdle}")
//    private int minIdle;
//    @Value("${datasource.maxActive}")
//    private int maxActive;


    /**
     * 用于获取  当前数据源的  key
     *
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {
        //多数据源请调用 goMultiDataSource() 方法
        //单数据源请调用 goSingleDataSource() 方法
        return goMultiDataSource();
    }


    /**
     * 走单数据源逻辑
     *
     * @return
     */
    private Object goSingleDataSource() {
        if (singleDataSourceKey == null) {
            DataSourceUtil.getInstance().changeDataSourceToMater();
            singleDataSourceKey = DataSourceContextHolder.getDB();
        }
        return singleDataSourceKey;
    }

    /**
     * 走多数据源逻辑
     *
     * @return
     */
    private Object goMultiDataSource() {
        String datasource = DataSourceContextHolder.getDB();

        /**
         * 由于  datasource 存在localThread 上,  新线程默认为空 , 所以 操作数据前必须先绑定数据源(其实就是切换哈数据源)
         *
         */
        if (null == datasource) {
            throw new RuntimeException("请先绑定数据源......当前已经注册的数据源有" + this.datasourceKeys);
        }

        return datasource;
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.dynamicTargetDataSources = targetDataSources;
    }


    /**
     * 切换数据源
     *
     * @return
     */
    public void switchDataSource(DataBaseMessage dataBaseMessage) {
        //获取连接基本信息
        String driveClass = dataBaseMessage.getDriverClass();
        if (StringUtils.isEmpty(driveClass)) {
            driveClass = "com.mysql.jdbc.Driver";
        }
        String url = dataBaseMessage.getUrl();
        String username = dataBaseMessage.getUserName();
        String password = dataBaseMessage.getPassword();
        //logger.debug("切换数据库的url:" + url);
        //开始切换数据源
        String key = "dataSource_" + dataBaseMessage.getDbName() + "_" + url.hashCode();
        if (datasourceKeys.contains(key)) {
            // 如果存在该数据源。就切换到该数据源
            changeDataSource(key);
            return;
        }
        try {
            try {
                // 排除连接不上的错误
                Class.forName(driveClass);
                // 测试 数据库连接
                Connection connection = DriverManager.getConnection(url, username, password);
                connection.close();
            } catch (Exception e) {
                logger.error("*********************你提供的数据源链接失败****************************", e);
                return;
            }
            @SuppressWarnings("resource")
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setName(key);
            druidDataSource.setDriverClassName(driveClass);
            druidDataSource.setUrl(url);
            druidDataSource.setUsername(username);
            druidDataSource.setPassword(password);
            druidDataSource.setMaxWait(60000);
            druidDataSource.setFilters("stat");

            druidDataSource.setInitialSize(1);//连接池初始大小
            druidDataSource.setMinIdle(1);//连接池最小数量
            druidDataSource.setMaxActive(20);//	连接池最大数量  默认 8
            DataSource createDataSource = (DataSource) druidDataSource;
            druidDataSource.init();
            Map<Object, Object> dynamicTargetDataSourcesNew = this.dynamicTargetDataSources;
            // 加入map
            dynamicTargetDataSourcesNew.put(key, createDataSource);

            //==========最后切数据源
            // 将map赋值给父类的TargetDataSources
            setTargetDataSources(dynamicTargetDataSourcesNew);
            // 将TargetDataSources中的连接信息放入resolvedDataSources管理
            super.afterPropertiesSet();

            datasourceKeys.add(key);
            changeDataSource(key);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 切换数据源Key
     *
     * @param key
     */
    private void changeDataSource(String key) {
        DataSourceContextHolder.setDB(key);
    }

//    /**
//     * 删除数据源
//     *
//     * @param datasourceid
//     * @return
//     */
//    public boolean delDatasources(String datasourceid) {
//        Map<Object, Object> dynamicTargetDataSources2 = this.dynamicTargetDataSources;
//        if (dynamicTargetDataSources2.containsKey(datasourceid)) {
//            Set<DruidDataSource> druidDataSourceInstances = DruidDataSourceStatManager.getDruidDataSourceInstances();
//            for (DruidDataSource l : druidDataSourceInstances) {
//                if (datasourceid.equals(l.getName())) {
//                    System.out.println(l);
//                    dynamicTargetDataSources2.remove(datasourceid);
//                    DruidDataSourceStatManager.removeDataSource(l);
//                    // 将map赋值给父类的TargetDataSources
//                    setTargetDataSources(dynamicTargetDataSources2);
//                    // 将TargetDataSources中的连接信息放入resolvedDataSources管理
//                    super.afterPropertiesSet();
//                    return true;
//                }
//            }
//            return false;
//        } else {
//            return false;
//        }
//    }
//
//
//    public Map<Object, Object> getDynamicTargetDataSources() {
//        return dynamicTargetDataSources;
//    }
//
//    public void setDynamicTargetDataSources(Map<Object, Object> dynamicTargetDataSources) {
//        this.dynamicTargetDataSources = dynamicTargetDataSources;
//    }


}
